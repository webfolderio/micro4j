package com.micro4j.maven.plugin;

import java.nio.file.Path;

import org.apache.maven.plugin.MojoExecutionException;
import org.sonatype.plexus.build.incremental.BuildContext;

public interface JsEngine {

    void init() throws MojoExecutionException;

    public boolean isInitialized();

    void dispose() throws MojoExecutionException;

    String execute(Path srcFile, String content, BuildContext buildContext) throws MojoExecutionException;
}
