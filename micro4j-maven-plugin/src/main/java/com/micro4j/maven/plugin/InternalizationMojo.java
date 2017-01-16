package com.micro4j.maven.plugin;

import static java.io.File.separator;
import static java.io.File.separatorChar;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.isDirectory;
import static java.nio.file.Files.readAllBytes;
import static org.apache.maven.plugins.annotations.LifecyclePhase.PROCESS_RESOURCES;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.Scanner;
import org.sonatype.plexus.build.incremental.BuildContext;

@Mojo(name = "i18n", defaultPhase = PROCESS_RESOURCES, threadSafe = true, requiresOnline = false, requiresReports = false)
public class InternalizationMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Parameter(readonly = true, required = true)
    private BuildContext buildContext;

    @Parameter(defaultValue = "key")
    private String enumField = "key";

    @Parameter(defaultValue = "**/*.properties")
    private String[] i18nIncludes = new String[] { "**/*.properties" };

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        for (Resource resource : project.getResources()) {
            File folder = new File(resource.getDirectory());
            if (isDirectory(folder.toPath())) {
                generate(folder);
            }
        }
    }

    protected void generate(File folder) throws MojoExecutionException {
        Scanner scanner = buildContext.newScanner(folder);
        scanner.setIncludes(i18nIncludes);
        scanner.scan();
        for (String includedFile : scanner.getIncludedFiles()) {
            String file = folder.getPath() + separator + includedFile;
            String content;
            try {
                content = new String(readAllBytes(new File(file).toPath()), UTF_8);
            } catch (IOException e) {
                throw new MojoExecutionException(e.getMessage(), e);
            }
            String packageName = "";
            String className = "";
            int start = includedFile.lastIndexOf(separatorChar);
            if (start <= 0) {
                packageName = includedFile;
            } else {
                packageName = includedFile.substring(0, start);
            }
            packageName = packageName.replace(separatorChar, '.');
            className = includedFile.substring(start + 1, includedFile.length());
            int end = className.indexOf('.');
            if (start > 0) {
                className = className.substring(0, end);
            }
            EnumGenerator generator = new EnumGenerator(packageName, className, enumField);
            String source = generator.generate(content);
            if (source == null || source.trim().isEmpty()) {
                continue;
            }
            String srcFolder = project.getBuild().getSourceDirectory();
            String srcPath = srcFolder +
                                separator +
                                packageName.replace('.', separatorChar) +
                                separatorChar +
                                className +
                                ".java";
            File srcFile = new File(srcPath);
            try {
                if (srcFile.exists() && srcFile.isFile()) {
                    String existingContent = new String(readAllBytes(srcFile.toPath()), UTF_8);
                    existingContent = existingContent.replaceAll("\\s+", "");
                    String newContent = source.replaceAll("\\s+", "");
                    if (existingContent.equals(newContent)) {
                        return;
                    }
                }
            } catch (IOException e) {
                throw new MojoExecutionException(e.getMessage(), e);
            }
            try (OutputStream os = buildContext.newFileOutputStream(srcFile)) {
                os.write(source.getBytes(UTF_8));
            } catch (IOException e) {
                throw new MojoExecutionException(e.getMessage(), e);
            }
        }
    }
}
