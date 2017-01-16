package com.micro4j.maven.plugin;

import static java.lang.Character.isUpperCase;
import static com.squareup.javapoet.JavaFile.builder;
import static com.squareup.javapoet.MethodSpec.constructorBuilder;
import static com.squareup.javapoet.TypeSpec.anonymousClassBuilder;
import static com.squareup.javapoet.TypeSpec.enumBuilder;
import static java.io.File.separator;
import static java.io.File.separatorChar;
import static java.lang.Character.isLetterOrDigit;
import static java.lang.Character.toLowerCase;
import static java.lang.Character.toUpperCase;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.isDirectory;
import static java.nio.file.Files.readAllBytes;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static org.apache.maven.plugins.annotations.LifecyclePhase.PROCESS_RESOURCES;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.Scanner;
import org.sonatype.plexus.build.incremental.BuildContext;

import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeSpec.Builder;

@Mojo(name = "i18n", defaultPhase = PROCESS_RESOURCES, threadSafe = true, requiresOnline = false, requiresReports = false)
public class InternalizationMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Component
    private BuildContext buildContext;

    @Parameter(defaultValue = "key")
    private String enumField = "key";

    @Parameter(defaultValue = "**/*.properties")
    private String[] i18nIncludes = new String[] { "**/*.properties" };

    private static final String EQUAL = "=";

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
                className = toCamelCase(className.substring(0, end));
            }
            String source = generate(content, packageName, className, enumField);
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
                        continue;
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

    protected static String toCamelCase(final String name) {
        final StringBuilder builder = new StringBuilder();
        boolean capitalizeNextChar = false;
        boolean first = true;
        for (int i = 0; i < name.length(); i++) {
            final char c = name.charAt(i);
            if (!isLetterOrDigit(c)) {
                if (!first) {
                    capitalizeNextChar = true;
                }
            } else {
                if (capitalizeNextChar || first || isUpperCase(c)) {
                    builder.append(toUpperCase(c));
                } else {
                    builder.append(toLowerCase(c));
                }
                capitalizeNextChar = false;
                first = false;
            }
        }
        return builder.toString();
    }

    protected String generate(String content,
                                    String packageName,
                                    String className,
                                    String fieldName) throws MojoExecutionException {
        List<String> keys = new ArrayList<>();
        try (java.util.Scanner scanner = new java.util.Scanner(content)) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                int start = line.indexOf(EQUAL);
                if (start <= 0) {
                    continue;
                }
                String key = line.substring(0, start);
                keys.add(key);
            }
        }
        Builder builder = enumBuilder(className)
                                .addField(String.class, fieldName, FINAL, PUBLIC)
                                .addMethod(
                                            constructorBuilder()
                                                .addModifiers(PRIVATE)
                                                .addParameter(String.class, fieldName)
                                                .addStatement("this.$N = $N", fieldName, fieldName)
                                            .build())
                                .addModifiers(PUBLIC);

        for (String key : keys) {
            builder.addEnumConstant(
                            toCamelCase(key),
                                anonymousClassBuilder("$S", key.trim())
                            .build());
        }
        TypeSpec spec = builder.build();
        StringWriter writer = new StringWriter();
        try {
            builder(packageName, spec)
                    .build()
                    .writeTo(writer);
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
        return writer.toString();
    }
}
