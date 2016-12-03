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

import static org.apache.maven.plugins.annotations.LifecyclePhase.PACKAGE;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
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
        if (Files.exists(jarFile)) {
            Scanner scanner = buildContext.newScanner(new File(project.getBuild().getSourceDirectory()), true);
            scanner.setIncludes(trimJarIncludes);
            scanner.scan();
            Map<String, String> properties = new HashMap<>(); 
            properties.put("create", Boolean.FALSE.toString());
            properties.put("encoding", StandardCharsets.UTF_8.name()); 

            try (FileSystem fs = FileSystems.newFileSystem(URI.create("jar:" + jarFile.toUri().toString()), properties)) {
                Path root = fs.getPath("/");
                for (String includedFile : scanner.getIncludedFiles()) {
                    try {
                        List<String> lines = Files.readAllLines(Paths.get(includedFile));
                        for (String line : lines) {
                            line = line.trim();
                            if ( ! line.endsWith(".class") ) {
                                continue;
                            }
                            Path classFile = root.resolve(line);
                            if (Files.exists(classFile)) {
                                Files.delete(classFile);
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
