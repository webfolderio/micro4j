package com.micro4j.maven.plugin;

import static org.apache.maven.plugins.annotations.LifecyclePhase.PROCESS_RESOURCES;

import java.nio.file.Path;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.sonatype.plexus.build.incremental.BuildContext;

@Mojo(name = "babel", defaultPhase = PROCESS_RESOURCES, threadSafe = true, requiresOnline = false, requiresReports = false)
public class BabelMojo extends BaseMojo {

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

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Component
    private BuildContext buildContext;

    @Override
    protected void init() throws MojoExecutionException {
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
    protected boolean supportsExtensionRenaming() {
        return true;
    }

    @Override
    protected String transform(Path srcFile, Path targetFile, String content) throws MojoExecutionException {
        buildContext.removeMessages(srcFile.toFile());
        JsEngine engine = new V8Engine(babelLocation, babelPresets);
        try {
            if (engine != null) {
                engine.init();
            }
            if ( engine == null || ! engine.isInitialized() ) {
                engine = new NashornEngine(babelLocation, babelPresets);
                engine.init();
            }
            return engine.execute(srcFile, content, buildContext);
        } finally {
            if (engine != null && engine.isInitialized()) {
                engine.dispose();
            }
        }
    }
}
