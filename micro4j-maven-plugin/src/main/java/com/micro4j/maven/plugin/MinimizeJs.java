package com.micro4j.maven.plugin;

import static java.lang.Thread.currentThread;
import static org.apache.maven.plugins.annotations.LifecyclePhase.PROCESS_RESOURCES;
import static org.sonatype.plexus.build.incremental.BuildContext.SEVERITY_ERROR;
import static org.sonatype.plexus.build.incremental.BuildContext.SEVERITY_WARNING;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.IOUtil;
import org.sonatype.plexus.build.incremental.BuildContext;

import com.google.javascript.jscomp.CompilationLevel;
import com.google.javascript.jscomp.Compiler;
import com.google.javascript.jscomp.CompilerOptions;
import com.google.javascript.jscomp.JSError;
import com.google.javascript.jscomp.Result;
import com.google.javascript.jscomp.SourceFile;

@Mojo(name = "minimize-js", defaultPhase = PROCESS_RESOURCES, threadSafe = false, requiresOnline = false, requiresReports = false)
public class MinimizeJs extends BaseMojo {

    @Parameter(defaultValue = "**/*.js")
    private String[] closureIncludes = new String[] { "**/*.js" };

    @Parameter(defaultValue = "**/*.min.js")
    private String[] closureExcludes = new String[] { "**/*.min.js" };

    @Parameter(defaultValue = "${project.build.sourceEncoding}")
    private String closureEncoding = "utf-8";

    @Parameter(defaultValue = "min.js")
    private String closureOutputExtension;

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Component
    private BuildContext buildContext;

    @Override
    protected void init() throws MojoExecutionException {
    }

    @Override
    protected String getEncoding() {
        return closureEncoding;
    }

    @Override
    protected MavenProject getProject() {
        return project;
    }

    @Override
    protected String[] getIncludes() {
        return closureIncludes;
    }

    @Override
    protected String[] getExcludes() {
        return closureExcludes;
    }

    @Override
    protected BuildContext getBuildContext() {
        return buildContext;
    }

    @Override
    protected String getOutputExtension() {
        return closureOutputExtension;
    }

    @Override
    protected String transform(Path srcFile, String content) throws MojoExecutionException {
        URL jqueryExternURL = currentThread().getContextClassLoader().getResource("jquery-1.12_and_2.2.js");
        if (jqueryExternURL == null) {
            getLog().error("Unable to load jquery extern from [classpath:jquery-1.12_and_2.2.js]");
            return null;
        }
        String jqueryExtern = null;
        try (InputStream is = jqueryExternURL.openStream()) {
            jqueryExtern = IOUtil.toString(is);
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
        SourceFile sourceFile = SourceFile.fromCode(srcFile.toString(), content);
        Compiler compiler = new Compiler(System.err);
        CompilerOptions options = new CompilerOptions();
        CompilationLevel.SIMPLE_OPTIMIZATIONS.setOptionsForCompilationLevel(options);
        SourceFile jQueryExternFile = SourceFile.fromCode("jquery-1.12_and_2.2.js", jqueryExtern);
        Result result = compiler.compile(jQueryExternFile, sourceFile, options);
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
            return null;
        }
    }
}
