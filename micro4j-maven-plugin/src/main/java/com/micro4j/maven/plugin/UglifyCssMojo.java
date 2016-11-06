/* MIT License
 * 
 * Copyright (c) 2016 http://micro4j.com
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.micro4j.maven.plugin;

import static java.lang.String.valueOf;
import static java.lang.System.currentTimeMillis;
import static java.lang.Thread.currentThread;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.readAllBytes;
import static java.nio.file.Files.write;
import static java.util.Arrays.asList;
import static java.util.Collections.EMPTY_SET;
import static java.util.Collections.singleton;
import static org.apache.maven.plugins.annotations.LifecyclePhase.PROCESS_RESOURCES;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.compiler.util.scan.InclusionScanException;
import org.codehaus.plexus.compiler.util.scan.SimpleSourceInclusionScanner;
import org.codehaus.plexus.compiler.util.scan.SourceInclusionScanner;
import org.codehaus.plexus.compiler.util.scan.mapping.SuffixMapping;

@Mojo(name = "uglifycss", defaultPhase = PROCESS_RESOURCES, threadSafe = true, requiresOnline = false, requiresReports = false)
public class UglifyCssMojo extends AbstractMojo {

    @Parameter(defaultValue = "src/main/resources")
    private File resources;

    @Parameter(defaultValue = "target/classes")
    private File outputDirectory;

    @Parameter(defaultValue = "css")
    private String extension;

    @Parameter
    private String[] includes;

    @Parameter
    private String[] excludes;

    @Parameter(defaultValue = "${project.build.sourceEncoding}")
    private String encoding;

    @Parameter(defaultValue = "min.css")
    private String outputPrefix;

    @Parameter(defaultValue = "uglifycss-0.0.25.js")
    private String uglifycssLocation;

    private static Invocable engine;

    @SuppressWarnings("unchecked")
    public void execute() throws MojoExecutionException {
        Set<String> setIncludes = includes != null ? new HashSet<>(asList(includes)) : singleton("**/*." + extension);
        Set<String> setExcludes = excludes != null ? new HashSet<>(asList(excludes)) : EMPTY_SET;
        SourceInclusionScanner scanner = new SimpleSourceInclusionScanner(setIncludes, setExcludes);
        scanner.addSourceMapping(new SuffixMapping("." + extension, "." + extension));
        try {
            Set<File> files = scanner.getIncludedSources(resources, outputDirectory);
            for (File file : files) {
                Path relativePath = resources.toPath().relativize(file.toPath());
                Path outputPath = outputDirectory.toPath().resolve(relativePath);
                String fileName = outputPath.getFileName().toString();
                int begin = fileName.lastIndexOf('.');
                String minFileName = fileName;
                if (begin > 0) {
                    minFileName = fileName.substring(0, begin) + "." + outputPrefix;
                }
                String cssContent = new String(readAllBytes(file.toPath()), encoding);
                String minifiedContent = valueOf(getEngine().invokeFunction("micro4jUglifyCss", cssContent));
                getLog().info("Minimizing css content [" + relativePath.toString() + "]");
                write(outputPath.getParent().resolve(minFileName), minifiedContent.getBytes(encoding));
                getLog().info("css content minimized to [" + outputPath.getParent().resolve(minFileName).toString() + "]");
            }
        } catch (InclusionScanException | IOException | NoSuchMethodException | ScriptException e) {
            throw new MojoExecutionException(e.getMessage());
        }
    }

    protected static synchronized Invocable getEngine() {
        return engine;
    }

    protected void init() throws MojoFailureException {
        try {
            getLog().info("Initializing the uglifycss from [" + uglifycssLocation + "]");
            URL url = currentThread().getContextClassLoader().getResource(uglifycssLocation);
            if (url == null) {
                getLog().error("Unable to load uglifycss from [" + uglifycssLocation + "]");
            }
            if (url != null) {
                long start = currentTimeMillis();
                try (InputStream is = new BufferedInputStream(url.openStream())) {
                    ScriptEngineManager manager = new ScriptEngineManager(null);
                    ScriptEngine scriptEngine = manager.getEngineByExtension("js");
                    if (scriptEngine == null) {
                        getLog().error("Unable to instantiate JavaScript engine");
                        return;
                    }
                    scriptEngine.eval(new InputStreamReader(is, UTF_8.name()));
                    scriptEngine.eval("var micro4jUglifyCss = function(input) { return module.processString(input); }");
                    engine = (Invocable) scriptEngine;
                    getLog().info("uglifycss initialized [" + (currentTimeMillis() - start) + " ms]");
                } catch (ScriptException e) {
                    getLog().error(e);
                }
            }
        } catch (IOException e) {
            getLog().error(e);
            throw new MojoFailureException(e.getMessage(), e);
        }
    }
}
