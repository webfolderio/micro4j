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

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.Scanner;
import org.sonatype.plexus.build.incremental.BuildContext;

@Mojo(name = "minimize-css", defaultPhase = PROCESS_RESOURCES, threadSafe = true, requiresOnline = false, requiresReports = false)
public class MinimizeCssMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Parameter(defaultValue = "**/*.css")
    private String[] minimizeCssIncludes = new String[] { "**/*.css" };

    @Parameter(defaultValue = "**/*.min.css")
    private String[] minimizeCssExcludes = new String[] { "**/*.min.css" };

    @Parameter(defaultValue = "${project.build.sourceEncoding}")
    private String minimizeCssEncoding;

    @Parameter(defaultValue = "min.css")
    private String minimizeCssOutputPrefix;

    @Parameter(defaultValue = "uglifycss-0.0.25.js")
    private String uglifyCssLocation;

    @Component
    private BuildContext buildContext;

    private static Invocable engine;

    public void execute() throws MojoExecutionException, MojoFailureException {
        if (engine == null) {
            init();
        }
        if (project.getBuild().getOutputDirectory() != null) {
            File dir = new File(project.getBuild().getOutputDirectory());
            if (isDirectory(dir.toPath())) {
                minimize(dir);
            }
        }
        if (project.getBuild().getTestOutputDirectory() != null) {
            File dir = new File(project.getBuild().getTestOutputDirectory());
            if (isDirectory(dir.toPath())) {
                minimize(dir);
            }
        }
    }

    protected void minimize(File dir) throws MojoExecutionException {
        boolean incremental = buildContext.isIncremental();
        boolean ignoreDelta = incremental ? false : true;
        Scanner scanner = buildContext.newScanner(dir, ignoreDelta);
        scanner.setIncludes(minimizeCssIncludes);
        if (minimizeCssExcludes != null && minimizeCssExcludes.length > 0) {
            scanner.setExcludes(minimizeCssExcludes);
        }
        scanner.scan();
        for (String includedFile : scanner.getIncludedFiles()) {
            Path cssFile = dir.toPath().resolve(includedFile);
            String es6FileName = cssFile.getFileName().toString();
            int begin = es6FileName.lastIndexOf('.');
            if (begin < 0) {
                continue;
            }
            String es5FileName = es6FileName.substring(0, begin) + "." + minimizeCssOutputPrefix;
            Path cssMinFile = cssFile.getParent().resolve(es5FileName);
            Path baseDir = scanner.getBasedir().toPath();
            String outputDir = project.getBuild().getOutputDirectory();
            cssMinFile = new File(outputDir).toPath().resolve(baseDir.relativize(cssMinFile));
            boolean isUptodate = buildContext.isUptodate(cssMinFile.toFile(), cssFile.toFile());
            if (!isUptodate) {
                minimize(cssFile, cssMinFile);
            }
        }
    }

    protected void minimize(Path cssFile, Path minCssFile) throws MojoExecutionException {
        String cssContent;
        try {
            cssContent = new String(readAllBytes(cssFile), minimizeCssEncoding);
            String minifiedContent = valueOf(getEngine().invokeFunction("micro4jUglifyCss", cssContent));
            getLog().info("Minimizing css content [" + cssFile.toString() + "]");
            write(minCssFile, minifiedContent.getBytes(minimizeCssEncoding));
            getLog().info("css content minimized to [" + minCssFile.toString() + "]");
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        } catch (NoSuchMethodException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        } catch (ScriptException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    protected static synchronized Invocable getEngine() {
        return engine;
    }

    protected void init() throws MojoFailureException {
        try {
            getLog().info("Initializing the uglifycss from [" + uglifyCssLocation + "]");
            URL url = currentThread().getContextClassLoader().getResource(uglifyCssLocation);
            if (url == null) {
                getLog().error("Unable to load uglifycss from [" + uglifyCssLocation + "]");
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
