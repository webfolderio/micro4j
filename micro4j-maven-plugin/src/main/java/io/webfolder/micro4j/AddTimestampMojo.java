/**
 * The MIT License
 * Copyright © 2016 - 2017 WebFolder OÜ
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package io.webfolder.micro4j;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.delete;
import static java.nio.file.Files.exists;
import static java.nio.file.Files.move;
import static java.nio.file.Paths.get;
import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static org.apache.maven.plugins.annotations.LifecyclePhase.PREPARE_PACKAGE;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.UserDefinedFileAttributeView;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.Scanner;
import org.sonatype.plexus.build.incremental.BuildContext;

@Mojo(name = "add-timestamp", defaultPhase = PREPARE_PACKAGE, threadSafe = true, requiresOnline = false, requiresReports = false)
public class AddTimestampMojo extends AbstractMojo {

    @Parameter(defaultValue = "**/*-combined.min.js, **/*-combined.min.css")
    private String[] addTimestampIncludes = new String[] { "**/*-combined.min.js", "**/*-combined.min.css" };

    @Parameter
    private String[] addTimestampExcludes;

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Parameter
    private boolean deleteExistingTimestampFiles = true;

    @Component
    private BuildContext buildContext;

    private final boolean supportsUserFileAttribute = FileSystems.getDefault().supportedFileAttributeViews().contains("user");

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        String timestamp = project.getProperties().getProperty("timestamp");
        if (timestamp == null || timestamp.trim().isEmpty()) {
            return;
        }
        String outputDirectory = project.getBuild().getOutputDirectory();
        if (outputDirectory == null) {
            return;
        }
        Path outputDir = get(outputDirectory);
        if (!exists(outputDir)) {
            return;
        }
        boolean ignoreDelete = true;
        Scanner scanner = buildContext.newScanner(outputDir.toFile(), ignoreDelete);
        scanner.setIncludes(addTimestampIncludes);
        if (addTimestampExcludes != null && addTimestampExcludes.length > 0) {
            scanner.setExcludes(addTimestampExcludes);
        }
        scanner.scan();
        for (String includedFile : scanner.getIncludedFiles()) {
            Path file = outputDir.resolve(includedFile);
            Path fileName = file.getFileName();
            Path renamedFile = file.getParent().resolve(timestamp + "-" + fileName.toString());
            if (supportsUserFileAttribute) {
                UserDefinedFileAttributeView attributes = Files.getFileAttributeView(file, UserDefinedFileAttributeView.class);
                try {
                    if (attributes.list().contains("added-timestamp")) {
                        if (deleteExistingTimestampFiles) {
                            delete(file);
                        }
                        continue;
                    }
                } catch (IOException e) {
                    getLog().warn(e.getMessage(), e);
                }
            }
            try {
                if (exists(renamedFile)) {
                    delete(renamedFile);
                }
                move(file, renamedFile, ATOMIC_MOVE);

                if (supportsUserFileAttribute) {
                    UserDefinedFileAttributeView attributes = Files.getFileAttributeView(renamedFile, UserDefinedFileAttributeView.class);
                    attributes.write("added-timestamp", UTF_8.encode("true"));
                }
            } catch (IOException e) {
                getLog().error(e.getMessage(), e);
                throw new MojoExecutionException(e.getMessage());
            }
        }
    }
}
