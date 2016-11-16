package com.micro4j.maven.plugin;

import static java.lang.System.currentTimeMillis;
import static java.lang.Thread.currentThread;
import static java.nio.charset.Charset.forName;
import static java.nio.file.Files.isDirectory;
import static java.nio.file.Files.readAllBytes;
import static java.nio.file.Files.write;
import static org.apache.maven.plugins.annotations.LifecyclePhase.PROCESS_RESOURCES;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;

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

import com.google.javascript.jscomp.CompilationLevel;
import com.google.javascript.jscomp.Compiler;
import com.google.javascript.jscomp.CompilerOptions;
import com.google.javascript.jscomp.Result;
import com.google.javascript.jscomp.SourceFile;

@Mojo(name = "closure", defaultPhase = PROCESS_RESOURCES, threadSafe = false, requiresOnline = false, requiresReports = false)
public class ClosureMojo extends AbstractMojo {

    @Parameter(defaultValue = "**/*.js")
    private String[] closureIncludes = new String[] { "**/*.js" };

    @Parameter(defaultValue = "**/*.min.js")
    private String[] closureExcludes = new String[] { "**/*.min.js" };

    @Parameter(defaultValue = "${project.build.sourceEncoding}")
    private String closureEncoding = "utf-8";

    @Parameter(defaultValue = "min.js")
    private String closureOutputPrefix;

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Component
    private BuildContext buildContext;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (project.getBuild().getSourceDirectory() != null) {
            File dir = new File(project.getBuild().getSourceDirectory());
            if (isDirectory(dir.toPath())) {
                transform(dir);
            }
        }
        if (project.getBuild().getTestSourceDirectory() != null) {
            File dir = new File(project.getBuild().getTestSourceDirectory());
            if (isDirectory(dir.toPath())) {
                transform(dir);
            }
        }
    }

    protected void transform(File dir) throws MojoExecutionException, MojoFailureException {
        boolean incremental = buildContext.isIncremental();
        boolean ignoreDelta = incremental ? false : true;
        Scanner scanner = buildContext.newScanner(dir, ignoreDelta);
        scanner.setIncludes(closureIncludes);
        if (closureExcludes != null && closureExcludes.length > 0) {
            scanner.setExcludes(closureExcludes);
        }
        scanner.scan();
        for (String includedFile : scanner.getIncludedFiles()) {
            Path file = dir.toPath().resolve(includedFile);
            String fileName = file.getFileName().toString();
            int begin = fileName.lastIndexOf('.');
            if (begin < 0) {
                continue;
            }
            String minFileName = fileName.substring(0, begin) + "." + closureOutputPrefix;
            Path minFile = file.getParent().resolve(minFileName);
            Path baseDir = scanner.getBasedir().toPath();
            String outputDir = project.getBuild().getOutputDirectory();
            minFile = new File(outputDir).toPath().resolve(baseDir.relativize(minFile));
            boolean isUptodate = buildContext.isUptodate(minFile.toFile(), file.toFile());
            if ( ! isUptodate ) {
                minify(file, minFile);
            }
        }
    }

    protected void minify(Path jsFile, Path minFile) throws MojoExecutionException, MojoFailureException {
        long start = currentTimeMillis();        
        URL jqueryExternURL = currentThread().getContextClassLoader().getResource("jquery-1.12_and_2.2.js");
        if (jqueryExternURL == null) {
            getLog().error("Unable to load jquery extern from [classpath:jquery-1.12_and_2.2.js]");
            return;
        }
        String jqueryExtern = null;
        try (InputStream is = jqueryExternURL.openStream()) {
            jqueryExtern = IOUtil.toString(is);
        } catch (IOException e) {
            throw new MojoFailureException(e.getMessage(), e);
        }
        getLog().info("Minifying  javascript file [" + jsFile.toString() + "] to [" + minFile.toString() + "]");
        String jsContent = null;
        try {
            jsContent = new String(readAllBytes(jsFile), forName(closureEncoding));
        } catch (IOException e) {
            getLog().error(e);
            throw new MojoExecutionException("Unable to read the file [" + jsFile.toString() + "]", e);
        }
        SourceFile sourceFile = SourceFile.fromCode(jsFile.toString(), jsContent);
        Compiler compiler = new Compiler(System.err);
        CompilerOptions options = new CompilerOptions();
        CompilationLevel.SIMPLE_OPTIMIZATIONS.setOptionsForCompilationLevel(options);
        SourceFile jQueryExternFile = SourceFile.fromCode("jquery-1.12_and_2.2.js", jqueryExtern);
        Result result = compiler.compile(jQueryExternFile, sourceFile, options);
        if (result.success) {
            try {
                write(minFile, compiler.toSource().getBytes(closureEncoding));
            } catch (IOException e) {
                throw new MojoFailureException(e.getMessage(), e);
            }
            getLog().info("Minification done [" + (currentTimeMillis() - start) + " ms]");
        } else {
            getLog().error("Minification failed");
        }
    }
}
