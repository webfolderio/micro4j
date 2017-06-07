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

@Mojo(name = "combine-js", defaultPhase = PREPARE_PACKAGE, threadSafe = true, requiresOnline = false, requiresReports = false)
public class CombineJsMojo extends AbstractCombineMojo {

    @Parameter(defaultValue = "**/*-combined.js, **/*-combined.min.js")
    private String[] combineJsIncludes = new String[] { "**/*-combined.js", "**/*-combined.min.js" };

    @Parameter
    private String[] combineJsExcludes;

    @Parameter(defaultValue = "js")
    private String combineJsOutputExtension = "js";

    @Parameter(defaultValue = "${project.build.sourceEncoding}")
    private String combineJsEncoding;

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Component
    private BuildContext buildContext;

    private static final Pattern[] REQUIRE_PATTERNS = new Pattern[] {
            compile("\\s*var\\s+.*\\s*=\\s*require\\(['|\"](?<path>.*?)['|\"]\\)", DOTALL)
    };

    @Override
    protected void init() {
    }

    @Override
    protected String getEncoding() {
        return combineJsEncoding;
    }

    @Override
    protected MavenProject getProject() {
        return project;
    }

    @Override
    protected String[] getIncludes() {
        return combineJsIncludes;
    }

    @Override
    protected String[] getExcludes() {
        return combineJsExcludes;
    }

    @Override
    protected BuildContext getBuildContext() {
        return buildContext;
    }

    @Override
    protected String getOutputExtension() {
        return combineJsOutputExtension;
    }

    protected Path getCacheDirectory() {
        return null;
    }

    @Override
    protected Pattern[] getPatterns() {
        return REQUIRE_PATTERNS;
    }
}
