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

import static java.lang.Boolean.TRUE;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.maven.plugins.annotations.LifecyclePhase.PACKAGE;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.sonatype.plexus.build.incremental.BuildContext;
import static java.nio.file.Files.*;

@Mojo(name = "zip", defaultPhase = PACKAGE, threadSafe = true, requiresOnline = false, requiresReports = false)
public class ZipMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Component
    private BuildContext buildContext;

    @Parameter(defaultValue = "${finalName}", required = false)
    private String zipFileName;

    private String[] zipEntries;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        Map<String, String> properties = new HashMap<>(); 
        properties.put("create", TRUE.toString());
        properties.put("encoding", UTF_8.name());
        String finalName = zipFileName == null || zipFileName.trim().isEmpty() ? project.getBuild().getFinalName() : zipFileName;
        Path outPath = Paths.get(project.getBuild().getDirectory());
        Path jarFile = outPath.resolve(finalName + ".zip");
        if (exists(jarFile)) {
            try {
                delete(jarFile);
            } catch (IOException e) {
                throw new MojoExecutionException(e.getMessage(), e);
            }
        }
        Path base = project.getBasedir().toPath().normalize().toAbsolutePath();
        try (FileSystem fs = FileSystems.newFileSystem(URI.create("jar:" + jarFile.toUri().toString()), properties)) {
            Path root = fs.getPath("/");
            for (String entry : zipEntries) {
                Path p = base.resolve(entry);
                if ( ! exists(p) ) {
                    createDirectories(root.resolve(p));
                }
            }
            for (String entry : zipEntries) {
                Path p = base.resolve(entry);
                if (exists(p) && isRegularFile(p)) {
                    copy(p, root.resolve(p));
                }
            }
        } catch (IOException e) {
            getLog().error(e.getMessage(), e);
        }
    }
}
