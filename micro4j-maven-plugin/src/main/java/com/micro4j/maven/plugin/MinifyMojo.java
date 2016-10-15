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

import com.google.javascript.jscomp.CompilationLevel;
import com.google.javascript.jscomp.Compiler;
import com.google.javascript.jscomp.CompilerOptions;
import com.google.javascript.jscomp.Result;
import com.google.javascript.jscomp.SourceFile;

@Mojo(name = "minify", defaultPhase = PROCESS_RESOURCES, threadSafe = false, requiresOnline = false, requiresReports = false)
public class MinifyMojo extends AbstractMojo {

    @Parameter(defaultValue = "**/*.js")
    private String[] includes = new String[] { "**/*.js" };

    @Parameter
    private String[] excludes;

    @Parameter(defaultValue = "${project.build.sourceEncoding}")
    private String encoding = "utf-8";

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Parameter(defaultValue = "min.js")
    private String outputPrefix;

    @Component
    private BuildContext buildContext;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        for (Resource resource : project.getResources()) {
            File folder = new File(resource.getDirectory());
            if (isDirectory(folder.toPath())) {
                minify(folder, false);
            }
        }
        for (Resource resource : project.getTestResources()) {
            File folder = new File(resource.getDirectory());
            if (isDirectory(folder.toPath())) {
                minify(folder, false);
            }
        }
    }

    protected void minify(File folder, boolean isTestFolder) throws MojoExecutionException, MojoFailureException {
        boolean incremental = buildContext.isIncremental();
        boolean ignoreDelta = incremental ? false : true;
        Scanner scanner = buildContext.newScanner(folder, ignoreDelta);
        scanner.setIncludes(includes);
        if (excludes != null && excludes.length > 0) {
            scanner.setExcludes(excludes);
        }
        scanner.scan();
        for (String includedFile : scanner.getIncludedFiles()) {
            Path jsFile = folder.toPath().resolve(includedFile);
            String jsFileName = jsFile.getFileName().toString();
            int begin = jsFileName.lastIndexOf('.');
            if (begin < 0) {
                continue;
            }
            String minFileName = jsFileName.substring(0, begin) + "." + outputPrefix;
            Path minFile = jsFile.getParent().resolve(minFileName);
            Path baseDir = scanner.getBasedir().toPath();
            String outputDir = project.getBuild().getOutputDirectory();
            minFile = new File(outputDir).toPath().resolve(baseDir.relativize(minFile));
            boolean isUptodate = buildContext.isUptodate(minFile.toFile(), jsFile.toFile());
            if ( ! isUptodate ) {
                minify(jsFile, minFile);
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
            jsContent = new String(readAllBytes(jsFile), forName(encoding));
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
                write(minFile, compiler.toSource().getBytes(encoding));
            } catch (IOException e) {
                throw new MojoFailureException(e.getMessage(), e);
            }
            getLog().info("Minification done [" + (currentTimeMillis() - start) + " ms]");
        } else {
            getLog().error("Minification failed");
        }
    }
}
