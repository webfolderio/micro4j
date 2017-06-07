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
import static java.nio.file.Files.copy;
import static java.nio.file.Files.delete;
import static java.nio.file.Files.exists;
import static java.nio.file.Files.readAllBytes;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.maven.plugin.MojoExecutionException;

public abstract class AbstractCombineMojo extends BaseMojo {

    abstract protected Pattern[] getPatterns();

    protected void beforeProcess(Path srcOrgFile, Path targetOrgFile) throws MojoExecutionException {
        if (exists(targetOrgFile)) {
            try {
                delete(targetOrgFile);
            } catch (IOException e) {
                getLog().error(e.getMessage(), e);
                throw new MojoExecutionException(e.getMessage());
            }
        }
        try {
            copy(srcOrgFile, targetOrgFile);
        } catch (IOException e) {
            getLog().error(e.getMessage(), e);
            throw new MojoExecutionException(e.getMessage());
        }
    }

    @Override
    protected String transform(Path srcFile, Path targetFile, String content) throws MojoExecutionException {
        List<String> imports = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new StringReader(content))) {
            List<String> lines = reader.lines().collect(Collectors.toList());
            for (String next : lines) {
                for (String line : next.split(";")) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("//") || line.startsWith("/*")) {
                        continue;
                    }
                    for (Pattern pattern : getPatterns()) {
                        Matcher matcher = pattern.matcher(line);
                        while (matcher.find()) {
                            if (matcher.matches()) {
                                if (matcher.groupCount() > 0) {
                                    String group = matcher.group("path");
                                    if ( group != null && ! group.trim().isEmpty() ) {
                                        group = group.trim();
                                        imports.add(group);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            getLog().error(e.getMessage(), e);
            throw new MojoExecutionException(e.getMessage());
        }
        StringBuilder builder = new StringBuilder(1024 * 16);
        for (String imprt : imports) {
            Path file = targetFile.getParent().resolve(imprt);
            if ( exists(file) ) {
                try {
                    String fileContent = new String(readAllBytes(file), UTF_8.name());
                    if ( ! fileContent.trim().isEmpty() ) {
                        builder.append(fileContent);
                        builder.append("\r\n");
                    }
                } catch (IOException e) {
                    getLog().error(e.getMessage(), e);
                    throw new MojoExecutionException(e.getMessage());
                }
            }
        }
        String combined = builder.toString();
        return combined.trim().isEmpty() ? null : combined;
    }

    protected Path getCacheDirectory() {
        return null;
    }
}
