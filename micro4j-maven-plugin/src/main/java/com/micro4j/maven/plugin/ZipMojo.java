/* MIT License
 * 
 * Copyright (c) 2016 - 2017 http://micro4j.com
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
import static java.nio.file.Files.copy;
import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.delete;
import static java.nio.file.Files.exists;
import static java.nio.file.Files.isRegularFile;
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

@Mojo(name = "zip", defaultPhase = PACKAGE, threadSafe = true, requiresOnline = false, requiresReports = false)
public class ZipMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Component
    private BuildContext buildContext;

    @Parameter(defaultValue = "${finalName}", required = false)
    private String zipFileName;

    @Parameter
    private String[] zipEntries;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        Map<String, String> properties = new HashMap<>(); 
        properties.put("create", TRUE.toString());
        properties.put("encoding", UTF_8.name());
        String finalName = zipFileName == null || zipFileName.trim().isEmpty() ? project.getBuild().getFinalName() : zipFileName;
        Path outPath = Paths.get(project.getBuild().getDirectory());
        Path jarFile = outPath.resolve(finalName + ".zip");
        if (exists(jarFile) && isRegularFile(jarFile)) {
            try {
                delete(jarFile);
            } catch (IOException e) {
                throw new MojoExecutionException(e.getMessage(), e);
            }
        }
        Path base = project.getBasedir().toPath().normalize().toAbsolutePath();
        try (FileSystem fs = FileSystems.newFileSystem(URI.create("jar:" + jarFile.toUri().toString()), properties)) {
            Path root = createDirectories(fs.getPath("/").resolve(finalName));
            for (String entry : zipEntries) {
                String[] arr = entry.split(":");
                Path src     = arr.length == 2 ? base.resolve(arr[0]) : base.resolve(entry);
                Path dest    = arr.length == 2 ? root.resolve(arr[1]) : root.resolve(entry);
                if ( ! exists(src) ) {
                    createDirectories(root.resolve(dest));
                }
                if (exists(src) && isRegularFile(src)) {
                    if ( ! exists(dest.getParent()) ) {
                        createDirectories(dest.getParent());
                    }
                    copy(src, dest);
                }
            }
        } catch (IOException e) {
            getLog().error(e.getMessage(), e);
        }
    }
}
