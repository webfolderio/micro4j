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

import static java.lang.Boolean.FALSE;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.delete;
import static java.nio.file.Files.exists;
import static java.nio.file.Files.readAllLines;
import static org.apache.maven.plugins.annotations.LifecyclePhase.PACKAGE;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.Scanner;
import org.sonatype.plexus.build.incremental.BuildContext;

@Mojo(name = "trim-jar", defaultPhase = PACKAGE, threadSafe = true, requiresOnline = false, requiresReports = false)
public class TrimJarMojo extends AbstractMojo {

    @Parameter
    private String[] trimJarIncludes = new String[] { };

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Component
    private BuildContext buildContext;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        String finalName = project.getBuild().getFinalName();
        Path outPath = Paths.get(project.getBuild().getDirectory());
        Path jarFile = outPath.resolve(finalName + ".jar");
        if (exists(jarFile)) {
            Scanner scanner = buildContext.newScanner(project.getBasedir(), true);
            scanner.setIncludes(trimJarIncludes);
            scanner.scan();
            Map<String, String> properties = new HashMap<>(); 
            properties.put("create", FALSE.toString());
            properties.put("encoding", UTF_8.name());
            try (FileSystem fs = FileSystems.newFileSystem(URI.create("jar:" + jarFile.toUri().toString()), properties)) {
                Path root = fs.getPath("/");
                for (String include : scanner.getIncludedFiles()) {
                    try {
                        List<String> lines = readAllLines(project.getBasedir().toPath().resolve(include));
                        for (String line : lines) {
                            line = line.trim();
                            if (line.isEmpty() || line.startsWith("#")) {
                                continue;
                            }
                            Path classFile = root.resolve(line);
                            if (exists(classFile)) {
                                delete(classFile);
                            }
                        }
                    } catch (IOException e) {
                        getLog().error(e.getMessage(), e);
                    }
                }
            } catch (IOException e) {
                getLog().error(e.getMessage(), e);
            }
        }
    }
}