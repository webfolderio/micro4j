package com.micro4j.maven.plugin;

import static java.nio.file.Files.isDirectory;
import static java.nio.file.Files.readAllBytes;
import static java.nio.file.Files.write;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.Scanner;
import org.sonatype.plexus.build.incremental.BuildContext;

public abstract class BaseMojo extends AbstractMojo {

    protected abstract void init();

    protected abstract String getEncoding();

    protected abstract MavenProject getProject();

    protected abstract String[] getIncludes();

    protected abstract String[] getExcludes();

    protected abstract BuildContext getBuildContext();

    protected abstract String getOutputExtension();

    protected abstract String transform(String content) throws MojoExecutionException;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        init();
        MavenProject project = getProject();
        if (project.getResources() != null) {
            for (Resource resource : project.getResources()) {
                File srcDir = new File(resource.getDirectory());
                if (isDirectory(srcDir.toPath())) {
                    process(srcDir, project.getBuild().getOutputDirectory());
                }
            }
        }
        if (project.getTestResources() != null) {
            for (Resource resource : project.getTestResources()) {
                File srcDir = new File(resource.getDirectory());
                if (isDirectory(srcDir.toPath())) {
                    process(srcDir, project.getBuild().getTestOutputDirectory());
                }
            }
        }
    }

    protected void process(File srcDir, String targetDir) throws MojoExecutionException {
        boolean incremental = getBuildContext().isIncremental();
        boolean ignoreDelta = incremental ? false : true;
        Scanner scanner = getBuildContext().newScanner(srcDir, ignoreDelta);
        scanner.setIncludes(getIncludes());
        if (getExcludes() != null && getExcludes().length > 0) {
            scanner.setExcludes(getExcludes());
        }
        scanner.scan();
        for (String includedFile : scanner.getIncludedFiles()) {
            Path srcFile = srcDir.toPath().resolve(includedFile);
            Path srcBaseDir = scanner.getBasedir().toPath();
            Path targetFile = new File(targetDir).toPath().resolve(srcBaseDir.relativize(srcFile));
            boolean isUptodate = getBuildContext().isUptodate(targetFile.toFile(), srcFile.toFile());
            if ( ! isUptodate ) {
                try {
                    String content = new String(readAllBytes(srcFile), getEncoding());
                    String modifiedContent = transform(content);
                    if (modifiedContent != null) {
                        if ( getOutputExtension() != null && ! getOutputExtension().trim().isEmpty() ) {
                            String fileName = targetFile.getFileName().toString();
                            int end = fileName.lastIndexOf(".");
                            if (end > 0) {
                                fileName = fileName.substring(0, end) + "." + getOutputExtension();
                                targetFile = targetFile.getParent().resolve(fileName);
                            }
                        }
                        write(targetFile, modifiedContent.getBytes(getEncoding()), TRUNCATE_EXISTING);
                    }
                } catch (IOException e) {
                    throw new MojoExecutionException(e.getMessage(), e);
                }
            }
        }
    }
}
