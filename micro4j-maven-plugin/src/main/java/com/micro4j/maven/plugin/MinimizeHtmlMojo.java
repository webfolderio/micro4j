package com.micro4j.maven.plugin;

import static org.apache.maven.plugins.annotations.LifecyclePhase.PROCESS_RESOURCES;
import static org.attoparser.config.ParseConfiguration.htmlConfiguration;
import static org.attoparser.minimize.MinimizeHtmlMarkupHandler.MinimizeMode.ONLY_WHITE_SPACE;

import java.io.StringWriter;
import java.nio.file.Path;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.attoparser.MarkupParser;
import org.attoparser.ParseException;
import org.attoparser.config.ParseConfiguration;
import org.attoparser.minimize.MinimizeHtmlMarkupHandler;
import org.attoparser.minimize.MinimizeHtmlMarkupHandler.MinimizeMode;
import org.attoparser.output.OutputMarkupHandler;
import org.sonatype.plexus.build.incremental.BuildContext;

@Mojo(name = "minimize-html", defaultPhase = PROCESS_RESOURCES, threadSafe = true, requiresOnline = false, requiresReports = false)
public class MinimizeHtmlMojo extends BaseMojo {

    @Parameter(defaultValue = "**/*.html")
    private String[] minimizeHtmlIncludes = new String[] { "**/*.html" };

    @Parameter
    private String[] minimizeHtmlExcludes;

    @Parameter(defaultValue = "html")
    private String minimizeHtmlOutputExtension = "html";

    @Parameter(defaultValue = "${project.build.sourceEncoding}")
    private String minimizeHtmlEncoding;

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Component
    private BuildContext buildContext;

    protected String transform(Path srcFile, Path targetFile, String content) throws MojoExecutionException {
        try {
            StringWriter writer = new StringWriter();
            OutputMarkupHandler outputHandler = new OutputMarkupHandler(writer);
            MinimizeHtmlMarkupHandler minimizeHandler = new MinimizeHtmlMarkupHandler(getMinimizeMode(),
                    outputHandler);
            MarkupParser parser = new MarkupParser(getParseConfiguration());
            parser.parse(content, minimizeHandler);
            return writer.toString();
        } catch (ParseException e) {
            getLog().error(e.getMessage(), e);
            throw new MojoExecutionException(e.getMessage());
        }
    }

    protected ParseConfiguration getParseConfiguration() {
        return htmlConfiguration();
    }

    protected MinimizeMode getMinimizeMode() {
        return ONLY_WHITE_SPACE;
    }

    @Override
    protected void init() {
    }

    @Override
    protected String getEncoding() {
        return minimizeHtmlEncoding;
    }

    @Override
    protected MavenProject getProject() {
        return project;
    }

    @Override
    protected String[] getIncludes() {
        return minimizeHtmlIncludes;
    }

    @Override
    protected String[] getExcludes() {
        return minimizeHtmlExcludes;
    }

    @Override
    protected BuildContext getBuildContext() {
        return buildContext;
    }

    @Override
    protected String getOutputExtension() {
        return minimizeHtmlOutputExtension;
    }

    protected Path getCacheDirectory() {
        return null;
    }
}
