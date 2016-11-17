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

import static java.lang.Integer.parseInt;
import static java.lang.String.valueOf;
import static java.lang.System.currentTimeMillis;
import static java.lang.Thread.currentThread;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.maven.plugins.annotations.LifecyclePhase.PROCESS_RESOURCES;
import static org.sonatype.plexus.build.incremental.BuildContext.SEVERITY_ERROR;

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

@Mojo(name = "babel-nashorn", defaultPhase = PROCESS_RESOURCES, threadSafe = true, requiresOnline = false, requiresReports = false)
public class BabelNashornMojo extends BaseMojo {

    @Parameter(defaultValue = "**/*.jsx, **/*.es6, **/*.es7, **/*.es")
    private String[] babelIncludes = new String[] { "**/*.jsx", "**/*.es6", "**/*.es7", "**/*.es" };

    @Parameter
    private String[] babelExcludes;

    @Parameter(defaultValue = "${project.build.sourceEncoding}")
    private String babelEncoding = "utf-8";

    @Parameter(defaultValue = "js")
    private String babelOutputExtension;

    @Parameter(defaultValue = "['es2015', 'stage-3']")
    private String babelPresets;

    @Parameter(defaultValue = "babel-standalone-6.18.1.min.js")
    private String babelLocation;

    @Component
    private BuildContext buildContext;

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    protected void init() {
    }

    @Override
    protected String getEncoding() {
        return babelEncoding;
    }

    @Override
    protected MavenProject getProject() {
        return project;
    }

    @Override
    protected String[] getIncludes() {
        return babelIncludes;
    }

    @Override
    protected String[] getExcludes() {
        return babelExcludes;
    }

    @Override
    protected BuildContext getBuildContext() {
        return buildContext;
    }

    @Override
    protected String getOutputExtension() {
        return babelOutputExtension;
    }

    @Override
    protected String transform(Path srcFile, String content) throws MojoExecutionException {
        buildContext.removeMessages(srcFile.toFile());
        String modifiedContent;
        try {
            modifiedContent = valueOf(getEngine().invokeFunction("micro4jCompile", content));
        } catch (NoSuchMethodException | ScriptException e) {
            buildContext.addMessage(srcFile.toFile(), 0, 0, e.getMessage(), SEVERITY_ERROR, null);
            getLog().error(e.getMessage(), e);
            throw new MojoExecutionException(e.getMessage(), e);
        }
        if (modifiedContent.startsWith("SyntaxError")) {
            int begin = modifiedContent.indexOf("(");
            int end = modifiedContent.indexOf(")");
            if (begin >= 0 && end > begin) {
                String[] position = modifiedContent.substring(begin + 1, end).split(":");
                int line = parseInt(position[0]);
                int col = parseInt(position[1]);
                buildContext.addMessage(srcFile.toFile(), line, col, modifiedContent, SEVERITY_ERROR, null);
            } else {
                getLog().error(modifiedContent);
            }
        }
        return modifiedContent;
    }

    protected Invocable getEngine() throws MojoExecutionException {
        try {
            getLog().info("Initializing the babel from [" + babelLocation + "]");
            URL url = currentThread().getContextClassLoader().getResource(babelLocation);
            if (url == null) {
                throw new MojoExecutionException("Unable to load babel from [" + babelLocation + "]");
            }
            long start = currentTimeMillis();
            try (InputStream is = new BufferedInputStream(url.openStream())) {
                ScriptEngineManager manager = new ScriptEngineManager(null);
                ScriptEngine scriptEngine = manager.getEngineByExtension("js");
                if (scriptEngine == null) {
                    throw new MojoExecutionException("Unable to instantiate JavaScript engine");
                }
                scriptEngine.eval(new InputStreamReader(is, UTF_8.name()));
                scriptEngine.eval("var micro4jCompile = function(input) { try { return Babel.transform(input, { presets: "
                                + babelPresets + " }).code; } catch(e) { return e;} }");
                getLog().info("Babel initialized [" + (currentTimeMillis() - start) + " ms]");
                return  (Invocable) scriptEngine;
            } catch (ScriptException e) {
                throw new MojoExecutionException(e.getMessage(), e);
            }
        } catch (IOException e) {
            getLog().error(e);
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }
}
