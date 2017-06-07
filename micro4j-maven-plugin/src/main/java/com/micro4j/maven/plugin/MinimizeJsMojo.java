/**
 * The MIT License
 * Copyright © 2016 - 2017 WebFolder OÜ
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.micro4j.maven.plugin;

import static java.lang.Thread.currentThread;
import static java.util.Arrays.asList;
import static org.apache.maven.plugins.annotations.LifecyclePhase.PROCESS_RESOURCES;
import static org.sonatype.plexus.build.incremental.BuildContext.SEVERITY_ERROR;
import static org.sonatype.plexus.build.incremental.BuildContext.SEVERITY_WARNING;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.IOUtil;
import org.sonatype.plexus.build.incremental.BuildContext;

import com.google.javascript.jscomp.AbstractCommandLineRunner;
import com.google.javascript.jscomp.CompilationLevel;
import com.google.javascript.jscomp.Compiler;
import com.google.javascript.jscomp.CompilerOptions;
import com.google.javascript.jscomp.CompilerOptions.LanguageMode;
import com.google.javascript.jscomp.JSError;
import com.google.javascript.jscomp.Result;
import com.google.javascript.jscomp.SourceFile;

@Mojo(name = "minimize-js", defaultPhase = PROCESS_RESOURCES, threadSafe = true, requiresOnline = false, requiresReports = false)
public class MinimizeJsMojo extends BaseMojo {

    @Parameter(defaultValue = "**/*.js, **/*.jsx, **/*.es6, **/*.es7, **/*.es")
    private String[] minimizeJsIncludes = new String[] { "**/*.js", "**/*.jsx", "**/*.es6", "**/*.es7", "**/*.es" };

    @Parameter(defaultValue = "**/*.min.js")
    private String[] minimizeJsExcludes = new String[] { "**/*.min.js" };

    @Parameter(defaultValue = "${project.build.sourceEncoding}")
    private String minimizeJsEncoding = "utf-8";

    @Parameter(defaultValue = "min.js")
    private String minimizeJsOutputExtension;

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Component
    private BuildContext buildContext;

    @Override
    protected void init() throws MojoExecutionException {
    }

    @Override
    protected String getEncoding() {
        return minimizeJsEncoding;
    }

    @Override
    protected MavenProject getProject() {
        return project;
    }

    @Override
    protected String[] getIncludes() {
        return minimizeJsIncludes;
    }

    @Override
    protected String[] getExcludes() {
        return minimizeJsExcludes;
    }

    @Override
    protected BuildContext getBuildContext() {
        return buildContext;
    }

    @Override
    protected String getOutputExtension() {
        return minimizeJsOutputExtension;
    }

    @Override
    protected String transform(Path srcFile, Path targetFile, String content) throws MojoExecutionException {
        URL jqueryExternURL = currentThread().getContextClassLoader().getResource("jquery-1.12_and_2.2.js");
        if (jqueryExternURL == null) {
            throw new MojoExecutionException("Unable to load jquery extern from [classpath:jquery-1.12_and_2.2.js]");
        }
        String jqueryExtern = null;
        try (InputStream is = jqueryExternURL.openStream()) {
            jqueryExtern = IOUtil.toString(is);
        } catch (IOException e) {
            getLog().error(e.getMessage(), e);
            throw new MojoExecutionException(e.getMessage());
        }
        SourceFile sourceFile = SourceFile.fromCode(srcFile.toString(), content);
        Compiler compiler = new Compiler(System.err);
        CompilerOptions options = new CompilerOptions();
        options.setLanguageIn(LanguageMode.ECMASCRIPT_NEXT);
        options.setLanguageOut(LanguageMode.ECMASCRIPT5);
        List<SourceFile> externs = new ArrayList<>();
        try {
            externs.addAll(AbstractCommandLineRunner.getBuiltinExterns(options.getEnvironment()));
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
        CompilationLevel.SIMPLE_OPTIMIZATIONS.setOptionsForCompilationLevel(options);
        SourceFile jQueryExternFile = SourceFile.fromCode("jquery-1.12_and_2.2.js", jqueryExtern);
        externs.add(jQueryExternFile);
        Result result = compiler.compile(externs, asList(sourceFile), options);
        buildContext.removeMessages(srcFile.toFile());
        for (JSError error : result.warnings) {
            buildContext.addMessage(srcFile.toFile(), error.lineNumber, error.getCharno(), error.description, SEVERITY_WARNING, null);
            getLog().warn(error.description);
        }
        if (result.success) {
            return compiler.toSource();
        } else {
            for (JSError error : result.errors) {
                buildContext.addMessage(srcFile.toFile(), error.lineNumber, error.getCharno(), error.description, SEVERITY_ERROR, null);
                getLog().error("Closure compilation error [" + srcFile.toString() + "] " + error.description);
            }
            if (result.errors != null && result.errors.length > 0) {
                throw new MojoExecutionException("Please fix closure compiler error(s)");
            }
            return null;
        }
    }
}
