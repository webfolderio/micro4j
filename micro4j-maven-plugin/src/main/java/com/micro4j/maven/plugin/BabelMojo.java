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
import static org.sonatype.plexus.build.incremental.BuildContext.SEVERITY_ERROR;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import static java.lang.Integer.parseInt;

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
    private String encoding = "utf-8";

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Parameter(defaultValue = "js")
    private String outputExtension;

    @Parameter(defaultValue = "['es2015', 'stage-3']")
    private String presets;

    @Parameter(defaultValue = "babel-standalone-6.7.7.min.js")
    private String babelLocation;

    @Component
    private BuildContext buildContext;

    private static Invocable engine;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (engine == null) {
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

    protected void transform(File folder, boolean isTestFolder) throws MojoExecutionException, MojoFailureException {
        boolean incremental = buildContext.isIncremental();
        boolean ignoreDelta = incremental ? false : true;
        Scanner scanner = buildContext.newScanner(folder, ignoreDelta);
        scanner.setIncludes(includes);
        if (excludes != null && excludes.length > 0) {
            scanner.setExcludes(excludes);
        }
        scanner.scan();
        for (String includedFile : scanner.getIncludedFiles()) {
            Path es6File = folder.toPath().resolve(includedFile);
            String es6FileName = es6File.getFileName().toString();
            int begin = es6FileName.lastIndexOf('.');
            if (begin < 0) {
                continue;
            }
            String es5FileName = es6FileName.substring(0, begin) + "." + outputExtension;
            Path es5File = es6File.getParent().resolve(es5FileName);
            Path baseDir = scanner.getBasedir().toPath();
            String outputDir = project.getBuild().getOutputDirectory();
            es5File = new File(outputDir).toPath().resolve(baseDir.relativize(es5File));
            boolean isUptodate = buildContext.isUptodate(es5File.toFile(), es6File.toFile());
            if (!isUptodate) {
                transform(es6File, es5File);
            }
        }
    }

    protected void transform(Path es6File, Path es5File) throws MojoExecutionException, MojoFailureException {
        long start = currentTimeMillis();
        getLog().info("Compiling  javascript file [" + es6File.toString() + "] to [" + es5File.toString() + "]");
        String es6content = null;
        try {
            es6content = new String(readAllBytes(es6File), forName(encoding));
        } catch (IOException e) {
            getLog().error(e);
            throw new MojoExecutionException("Unable to read the file [" + es6File.toString() + "]", e);
        }
        try {
            String es5Content = valueOf(getEngine().invokeFunction("micro4jCompile", es6content));
            if (es5Content.startsWith("SyntaxError")) {
                int begin = es5Content.indexOf("(");
                int end = es5Content.indexOf(")");
                if (begin >= 0 && end > begin) {
                    String[] position = es5Content.substring(begin + 1, end).split(":");
                    int line = parseInt(position[0]);
                    int col = parseInt(position[1]);
                    buildContext.addMessage(es6File.toFile(), line, col, es5Content, SEVERITY_ERROR, null);
                } else {
                    getLog().error(es5Content);
                }
                if (!buildContext.isIncremental()) {
                    throw new MojoFailureException("Javascript compilation error [" + es6File.toString() + "]");
                }
            } else {
                write(es5File, es5Content.getBytes());
                buildContext.removeMessages(es6File.toFile());
                getLog().info("Compilation done [" + (currentTimeMillis() - start) + " ms]");
            }
        } catch (NoSuchMethodException | ScriptException | IOException e) {
            getLog().error(e);
            throw new MojoExecutionException("Unable to conver esnext to es5", e);
        }
    }

    protected static synchronized Invocable getEngine() {
        return engine;
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
                    ScriptEngine scriptEngine = manager.getEngineByExtension("js");
                    if (scriptEngine == null) {
                        getLog().error("Unable to instantiate JavaScript engine");
                        return;
                    }
                    scriptEngine.eval(new InputStreamReader(is, UTF_8.name()));
                    scriptEngine
                            .eval("var micro4jCompile = function(input) { try { return Babel.transform(input, { presets: "
                                    + presets + " }).code; } catch(e) { return e;} }");
                    engine = (Invocable) scriptEngine;
                    getLog().info("Babel initialized [" + (currentTimeMillis() - start) + " ms]");
                } catch (ScriptException e) {
                    getLog().error(e);
                }
            }
        } catch (IOException e) {
            getLog().error(e);
        }
    }
}
