package com.micro4j.maven.plugin;

import static java.nio.file.Files.delete;
import static java.nio.file.Files.exists;
import static java.nio.file.Files.move;
import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static org.apache.maven.plugins.annotations.LifecyclePhase.PREPARE_PACKAGE;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

@Mojo(name = "move", defaultPhase = PREPARE_PACKAGE, threadSafe = true, requiresOnline = false, requiresReports = false)
public class MoveMojo extends AbstractMojo {

    @Parameter(required = true)
    private File moveSourceFile;

    @Parameter(required = true)
    private File moveDestinationFile;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (moveSourceFile == null || moveDestinationFile == null) {
            return;
        }
        Path src = moveSourceFile.toPath();
        Path dest = moveDestinationFile.toPath();
        try {
            if (exists(src)) {
                if (exists(dest)) {
                    delete(dest);
                }
                move(src, dest, ATOMIC_MOVE);
            }
        } catch (IOException e) {
            getLog().error(e.getMessage(), e);
            throw new MojoExecutionException(e.getMessage());
        }
    }
}
