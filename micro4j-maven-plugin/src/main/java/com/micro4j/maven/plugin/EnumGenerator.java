package com.micro4j.maven.plugin;

import static com.squareup.javapoet.JavaFile.builder;
import static com.squareup.javapoet.TypeSpec.enumBuilder;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.PRIVATE;
import static java.lang.Character.*;

import static com.squareup.javapoet.TypeSpec.anonymousClassBuilder;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.apache.maven.plugin.MojoExecutionException;

import static com.squareup.javapoet.MethodSpec.constructorBuilder;

import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeSpec.Builder;

class EnumGenerator {

    private String packageName;

    private String className;

    private String fieldName;

    private static final String EQUAL = "=";

    public EnumGenerator(String packageName, String className, String fieldName) {
        this.packageName = packageName;
        this.className = className;
        this.fieldName = fieldName;
    }

    public String generate(String content) throws MojoExecutionException {
        List<String> keys = new ArrayList<>();
        try (Scanner scanner = new Scanner(content)) {
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
            builder.addEnumConstant(toCamelCase(key),
                            anonymousClassBuilder("$S", key).build());
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
                builder.append(capitalizeNextChar || first ? toUpperCase(c) : toLowerCase(c));
                capitalizeNextChar = false;
                first = false;
            }
        }
        return builder.toString();
    }
}
