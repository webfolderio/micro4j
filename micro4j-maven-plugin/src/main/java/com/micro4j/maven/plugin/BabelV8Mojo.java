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
import static java.nio.charset.Charset.forName;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.isDirectory;
import static java.nio.file.Files.readAllBytes;
import static java.nio.file.Files.write;
import static org.apache.maven.plugins.annotations.LifecyclePhase.PROCESS_RESOURCES;
import static org.sonatype.plexus.build.incremental.BuildContext.SEVERITY_ERROR;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;

import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.Scanner;
import org.sonatype.plexus.build.incremental.BuildContext;

import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Array;

@Mojo(name = "babel-v8", defaultPhase = PROCESS_RESOURCES, threadSafe = false, requiresOnline = false, requiresReports = false)
public class BabelV8Mojo extends AbstractMojo {

    @Parameter(defaultValue = "**/*.jsx, **/*.es6, *.es7, **/*.es")
    private String[] babelIncludes = new String[] { "**/*.jsx", "**/*.es6", "*.es7", "**/*.es" };

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

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Component
    private BuildContext buildContext;

    private V8 runtime;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        init();
        try {
            for (Resource resource : project.getResources()) {
                File folder = new File(resource.getDirectory());
                if (isDirectory(folder.toPath())) {
                    transform(folder);
                }
            }
            for (Resource resource : project.getTestResources()) {
                File folder = new File(resource.getDirectory());
                if (isDirectory(folder.toPath())) {
                    transform(folder);
                }
            }
        } catch (Throwable t) {
            if ( runtime != null && ! runtime.isReleased() ) {
                runtime.release();
            }
            throw t;
        }
    }

    protected void transform(File folder) throws MojoExecutionException, MojoFailureException {
        boolean incremental = buildContext.isIncremental();
        boolean ignoreDelta = incremental ? false : true;
        Scanner scanner = buildContext.newScanner(folder, ignoreDelta);
        scanner.setIncludes(babelIncludes);
        if (babelExcludes != null && babelExcludes.length > 0) {
            scanner.setExcludes(babelExcludes);
        }
        scanner.scan();
        for (String next : scanner.getIncludedFiles()) {
            Path es6File = folder.toPath().resolve(next);
            String es6FileName = es6File.getFileName().toString();
            int begin = es6FileName.lastIndexOf('.');
            if (begin < 0) {
                continue;
            }
            String es5FileName = es6FileName.substring(0, begin) + "." + babelOutputExtension;
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
            es6content = new String(readAllBytes(es6File), forName(babelEncoding));
        } catch (IOException e) {
            getLog().error(e);
            throw new MojoExecutionException("Unable to read the file [" + es6File.toString() + "]", e);
        }
        try {
            V8Array arguments = new V8Array(runtime);
            arguments.push(es6content);
            String es5Content = valueOf(String.valueOf(runtime.executeFunction("micro4jCompile", arguments)));
            if ( ! arguments.isReleased() ) {
                arguments.release();
            }
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
        } catch (IOException e) {
            getLog().error(e);
            throw new MojoExecutionException("Unable to conver esnext to es5", e);
        }
    }

    protected void init() throws MojoFailureException {
        getLog().info("Initializing the babel from [" + babelLocation + "]");
        URL url = currentThread().getContextClassLoader().getResource(babelLocation);
        if (url == null) {
            getLog().error("Unable to load babel from [" + babelLocation + "]");
        }
        if (url != null) {
            long start = currentTimeMillis();
            try (InputStream is = new BufferedInputStream(url.openStream())) {
                runtime = V8.createV8Runtime();
                runtime.executeScript(IOUtil.toString(is, UTF_8.name()));
                runtime.executeScript("var micro4jCompile = function(input) { try { return Babel.transform(input, { presets: "
                        + babelPresets + " }).code; } catch(e) { return e;} }");
                getLog().info("Babel initialized [" + (currentTimeMillis() - start) + " ms]");
            } catch (Throwable e) {
                getLog().error(e.getMessage(), e);
                if (runtime != null) {
                    runtime.release();
                }
                throw new MojoFailureException(e.getMessage(), e);
            }
        }
    }
}
