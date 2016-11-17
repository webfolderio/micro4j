/* MIT License
 * 
 * Copyright (c) 2016 http://micro4j.com
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.micro4j.maven.plugin;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.exists;
import static java.nio.file.Files.readAllBytes;
import static java.util.regex.Pattern.DOTALL;
import static java.util.regex.Pattern.compile;
import static org.apache.maven.plugins.annotations.LifecyclePhase.PREPARE_PACKAGE;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.IOUtil;
import org.sonatype.plexus.build.incremental.BuildContext;

@Mojo(name = "combine-js", defaultPhase = PREPARE_PACKAGE, threadSafe = true, requiresOnline = false, requiresReports = false)
public class CombineJsMojo extends BaseMojo {

    @Parameter(defaultValue = "**/*-combined.js")
    private String[] combineJsIncludes = new String[] { "**/*-combined.js" };

    @Parameter
    private String[] combineJsExcludes;

    @Parameter(defaultValue = "js")
    private String combineJsOutputExtension = "js";

    @Parameter(defaultValue = "${project.build.sourceEncoding}")
    private String combineJsEncoding;

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Component
    private BuildContext buildContext;

    @Component
    private MavenSession mavenSession;

    private static final Pattern REQUIRE_PATTERN = compile("\\s*var\\s+.*\\s*=\\s*require\\(['|\"](.*?)['|\"]\\)", DOTALL);

    @Override
    protected void init() {
    }

    @Override
    protected String getEncoding() {
        return combineJsEncoding;
    }

    @Override
    protected MavenProject getProject() {
        return project;
    }

    @Override
    protected String[] getIncludes() {
        return combineJsIncludes;
    }

    @Override
    protected String[] getExcludes() {
        return combineJsExcludes;
    }

    @Override
    protected BuildContext getBuildContext() {
        return buildContext;
    }

    @Override
    protected String getOutputExtension() {
        return combineJsOutputExtension;
    }

    @Override
    protected String transform(Path srcFile, String content) throws MojoExecutionException {
        List<String> imports = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new StringReader(content))) {
            List<String> lines = reader.lines().collect(Collectors.toList());
            for (String next : lines) {
                for (String line : next.split(";")) {
                    line = line.trim();
                    Matcher matcher = REQUIRE_PATTERN.matcher(line);
                    while (matcher.find()) {
                        if (matcher.matches()) {
                            if (matcher.groupCount() > 0) {
                                String group = matcher.group(1);
                                group = group.trim();
                                if ( ! group.startsWith("//") && ! group.startsWith("/*") ) {
                                    imports.add(group);
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
        StringBuilder builder = new StringBuilder();
        for (String imprt : imports) {
            Path jsFile = srcFile.getParent().resolve(imprt);
            if ( exists(jsFile) ) {
                try {
                    String js = IOUtil.toString(readAllBytes(jsFile), UTF_8.name());
                    if ( ! js.trim().isEmpty() ) {
                        builder.append(js);
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
}
