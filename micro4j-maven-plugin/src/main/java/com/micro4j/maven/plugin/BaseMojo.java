package com.micro4j.maven.plugin;

import static java.nio.file.Files.copy;
import static java.nio.file.Files.exists;
import static java.nio.file.Files.isDirectory;
import static java.nio.file.Files.readAllBytes;
import static java.nio.file.Files.write;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.util.Collections.emptyMap;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.Scanner;
import org.sonatype.plexus.build.incremental.BuildContext;

public abstract class BaseMojo extends AbstractMojo {

    protected abstract void init() throws MojoExecutionException;

    protected abstract String getEncoding();

    protected abstract MavenProject getProject();

    protected abstract String[] getIncludes();

    protected abstract String[] getExcludes();

    protected abstract BuildContext getBuildContext();

    protected abstract String getOutputExtension();

    protected abstract String transform(Path srcFile, String content) throws MojoExecutionException;

    protected Map<String, String> getExtensionMappings() {
        return emptyMap();
    }

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
            Map<String, String> mappings = getExtensionMappings();
            if ( ! mappings.isEmpty() ) {
                String srcFileName = srcFile.getFileName().toString();
                int srcFileNameEnd = srcFileName.lastIndexOf(".");
                if (srcFileNameEnd > 0) {
                    String srcExtension = srcFileName.substring(srcFileNameEnd + 1, srcFileName.length());
                    String mappedExtension = mappings.get(srcExtension);
                    if (mappedExtension != null) {
                        srcFileName = srcFileName.substring(0, srcFileNameEnd) + "." + mappedExtension;
                        srcFile = srcFile.getParent().resolve(srcFileName);
                    }
                }
            }
            Path srcBaseDir = scanner.getBasedir().toPath();
            Path targetFile = new File(targetDir).toPath().resolve(srcBaseDir.relativize(srcFile));
            try {
                if ( ! exists(targetFile) && exists(srcFile) ) {
                    copy(srcFile, targetFile);
                }
                String content = new String(readAllBytes(targetFile), getEncoding());
                String modifiedContent = transform(srcFile, content);
                if (modifiedContent != null) {
                    if ( getOutputExtension() != null && ! getOutputExtension().trim().isEmpty() ) {
                        String targetFileName = targetFile.getFileName().toString();
                        int targetFileNameEnd = targetFileName.lastIndexOf(".");
                        if (targetFileNameEnd > 0) {
                            targetFileName = targetFileName.substring(0, targetFileNameEnd) + "." + getOutputExtension();
                            targetFile = targetFile.getParent().resolve(targetFileName);
                        }
                    }
                    write(targetFile, modifiedContent.getBytes(getEncoding()), CREATE, TRUNCATE_EXISTING);
                    getBuildContext().refresh(targetFile.toFile());
                }
            } catch (IOException e) {
                getLog().error(e);
            }
        }
    }
}
