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
import static org.apache.maven.plugins.annotations.LifecyclePhase.PROCESS_RESOURCES;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Path;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.sonatype.plexus.build.incremental.BuildContext;

@Mojo(name = "minimize-css", defaultPhase = PROCESS_RESOURCES, threadSafe = true, requiresOnline = false, requiresReports = false)
public class MinimizeCssMojo extends BaseMojo {

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Parameter(defaultValue = "**/*.css")
    private String[] minimizeCssIncludes = new String[] { "**/*.css" };

    @Parameter(defaultValue = "**/*.min.css")
    private String[] minimizeCssExcludes = new String[] { "**/*.min.css" };

    @Parameter(defaultValue = "${project.build.sourceEncoding}")
    private String minimizeCssEncoding;

    @Parameter(defaultValue = "min.css")
    private String minimizeCssOutputExtension;

    @Parameter(defaultValue = "uglifycss-0.0.25.js")
    private String uglifyCssLocation;

    @Component
    private BuildContext buildContext;

    private static Invocable engine;

    protected static synchronized Invocable getEngine() {
        return engine;
    }

    @Override
    protected void init() throws MojoExecutionException {
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
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    @Override
    protected String getEncoding() {
        return minimizeCssEncoding;
    }

    @Override
    protected MavenProject getProject() {
        return project;
    }

    @Override
    protected String[] getIncludes() {
        return minimizeCssIncludes;
    }

    @Override
    protected String[] getExcludes() {
        return minimizeCssExcludes;
    }

    @Override
    protected BuildContext getBuildContext() {
        return buildContext;
    }

    @Override
    protected String getOutputExtension() {
        return minimizeCssOutputExtension;
    }

    @Override
    protected String transform(Path srcFile, String content) throws MojoExecutionException {
        try {
            return valueOf(getEngine().invokeFunction("micro4jUglifyCss", content));
        } catch (NoSuchMethodException | ScriptException e) {
            getLog().error(e);
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }
}
