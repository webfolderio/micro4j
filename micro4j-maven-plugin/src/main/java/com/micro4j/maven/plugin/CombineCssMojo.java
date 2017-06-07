package com.micro4j.maven.plugin;

import static java.util.regex.Pattern.DOTALL;
import static java.util.regex.Pattern.compile;
import static org.apache.maven.plugins.annotations.LifecyclePhase.PREPARE_PACKAGE;

import java.nio.file.Path;
import java.util.regex.Pattern;

import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.sonatype.plexus.build.incremental.BuildContext;

@Mojo(name = "combine-css", defaultPhase = PREPARE_PACKAGE, threadSafe = true, requiresOnline = false, requiresReports = false)
public class CombineCssMojo extends AbstractCombineMojo {

    @Parameter(defaultValue = "**/*-combined.css, **/*-combined.min.css")
    private String[] combineCssIncludes = new String[] { "**/*-combined.css", "**/*-combined.min.css" };

    @Parameter
    private String[] combineCssExcludes;

    @Parameter(defaultValue = "css")
    private String combineCssOutputExtension = "css";

    @Parameter(defaultValue = "${project.build.sourceEncoding}")
    private String combineCssEncoding;

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Component
    private BuildContext buildContext;

    private static final Pattern[] IMPORT_PATTERNS = new Pattern[] {
            compile("\\s*@import\\s+.*\\s*url\\(['|\"](?<path>.*?)['|\"]\\)", DOTALL),
            compile("\\s*@import\\s+.*['|\"](?<path>.*?)['|\"]", DOTALL)
    };

    @Override
    protected void init() {
    }

    @Override
    protected String getEncoding() {
        return combineCssEncoding;
    }

    @Override
    protected MavenProject getProject() {
        return project;
    }

    @Override
    protected String[] getIncludes() {
        return combineCssIncludes;
    }

    @Override
    protected String[] getExcludes() {
        return combineCssExcludes;
    }

    @Override
    protected BuildContext getBuildContext() {
        return buildContext;
    }

    @Override
    protected String getOutputExtension() {
        return combineCssOutputExtension;
    }

    protected Path getCacheDirectory() {
        return null;
    }

    @Override
    protected Pattern[] getPatterns() {
        return IMPORT_PATTERNS;
    }
}
