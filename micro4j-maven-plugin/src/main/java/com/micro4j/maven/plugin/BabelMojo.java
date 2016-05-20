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
import static java.nio.charset.Charset.forName;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.isDirectory;
import static java.nio.file.Files.readAllBytes;
import static java.nio.file.Files.write;
import static org.apache.maven.plugins.annotations.LifecyclePhase.PROCESS_RESOURCES;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Path;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.Scanner;
import org.sonatype.plexus.build.incremental.BuildContext;

@Mojo(name = "babel", defaultPhase = PROCESS_RESOURCES, threadSafe = false, requiresOnline = false, requiresReports = false)
public class BabelMojo extends AbstractMojo {

    @Parameter(defaultValue = "**/*.jsx, **/*.es6, *.es7, **/*.es")
    private String[] includes = new String[] { "**/*.jsx", "**/*.es6", "*.es7", "**/*.es" };

    @Parameter
    private String[] excludes;

    @Parameter(defaultValue = "${project.build.sourceEncoding}")
    private String encoding;

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Parameter(defaultValue = "js")
    private String outputExtension;

    @Parameter(defaultValue = "babel-standalone-6.7.7.min.js")
    private String babelLocation;

    @Component
    private BuildContext buildContext;

    private Invocable invocable;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (invocable == null) {
            init();
        }
        for (Resource resource : project.getResources()) {
            File folder = new File(resource.getDirectory());
            if (isDirectory(folder.toPath())) {
                transform(folder, false);
            }
        }
        for (Resource resource : project.getTestResources()) {
            File folder = new File(resource.getDirectory());
            if (isDirectory(folder.toPath())) {
                transform(folder, true);
            }
        }
    }

    protected void transform(File file, boolean isTestFolder) throws MojoExecutionException {
        boolean incremental = buildContext.isIncremental();
        boolean ignoreDelta = incremental ? false : true;
        Scanner scanner = buildContext.newScanner(file, ignoreDelta);
        scanner.setIncludes(includes);
        if (excludes != null && excludes.length > 0) {
            scanner.setExcludes(excludes);
        }
        scanner.scan();
        for (String includedFile : scanner.getIncludedFiles()) {
            Path esNextFile = file.toPath().resolve(includedFile);
            String esNextFileName = esNextFile.getFileName().toString();
            int begin = esNextFileName.lastIndexOf('.');
            if (begin < 0) {
                continue;
            }
            String es5FileName = esNextFileName.substring(0, begin) + "." + outputExtension;
            Path es5File = esNextFile.getParent().resolve(es5FileName);
            Path baseDir = scanner.getBasedir().toPath();
            String outputDir = project.getBuild().getOutputDirectory();
            es5File = new File(outputDir).toPath().resolve(baseDir.relativize(es5File));
            transform(esNextFile, es5File);
        }
    }

    protected void transform(Path esNextFile, Path es5File) throws MojoExecutionException {
        long start = currentTimeMillis();
        getLog().info("Compiling  javascript file [" + esNextFile.toString() + "] to [" + es5File.toString() + "]");
        String esNextcontent = null;
        try {
            esNextcontent = new String(readAllBytes(esNextFile), forName(encoding));
        } catch (IOException e) {
            getLog().error(e);
            throw new MojoExecutionException("Unable to read the file [" + esNextFile.toString() + "]", e);
        }
        try {
            String es5Content = valueOf(invocable.invokeFunction("micro4jCompile", esNextcontent));
            write(es5File, es5Content.getBytes());
            getLog().info("Compilation done [" + (currentTimeMillis() - start) + " ms]");
        } catch (NoSuchMethodException | ScriptException | IOException e) {
            getLog().error(e);
            throw new MojoExecutionException("Unable to conver esnext to es5", e);
        }
    }

    protected void init() {
        try {
            getLog().info("Initializing the babel from [" + babelLocation + "]");
            URL url = currentThread().getContextClassLoader().getResource(babelLocation);
            if (url == null) {
                getLog().error("Unable to load babel from [" + babelLocation + "]");
            }
            if (url != null) {
                long start = currentTimeMillis();
                try (InputStream is = new BufferedInputStream(url.openStream())) {
                    ScriptEngineManager manager = new ScriptEngineManager(null);
                    ScriptEngine engine = manager.getEngineByExtension("js");
                    if (engine == null) {
                        getLog().error("Unable to instantiate JavaScript engine");
                        return;
                    }
                    engine.eval(new InputStreamReader(is, UTF_8.name()));
                    engine.eval("var micro4jCompile = function(input) { return Babel.transform(input, { presets: ['es2015', 'stage-3'] }).code; }");
                    invocable = (Invocable) engine;
                    getLog().info("babel initialized [" + (currentTimeMillis() - start) + " ms]");
                } catch (ScriptException e) {
                    getLog().error(e);
                }
            }
        } catch (IOException e) {
            getLog().error(e);
        }
    }
}
