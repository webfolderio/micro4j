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

import static java.nio.file.Files.delete;
import static java.nio.file.Files.exists;
import static org.apache.maven.plugins.annotations.LifecyclePhase.PREPARE_PACKAGE;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.Scanner;
import org.sonatype.plexus.build.incremental.BuildContext;

@Mojo(name = "delete", defaultPhase = PREPARE_PACKAGE, threadSafe = true, requiresOnline = false, requiresReports = false)
public class DeleteMojo extends AbstractMojo {

    @Parameter
    private String[] deleteIncludes = new String[] { };

    @Parameter
    private String[] deleteExcludes = new String[] { };

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Component
    private BuildContext buildContext;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        String deleteDirectory = project.getBuild().getOutputDirectory();
        if (deleteDirectory == null) {
            return;
        }
        Path deleteDir = Paths.get(deleteDirectory);
        if ( ! exists(deleteDir) ) {
            return;
        }
        boolean ignoreDelete = true;
        Scanner scanner = buildContext.newScanner(deleteDir.toFile(), ignoreDelete);
        scanner.setIncludes(deleteIncludes);
        if ( deleteExcludes != null && deleteExcludes.length > 0 ) {
            scanner.setExcludes(deleteExcludes);
        }
        scanner.scan();
        for (String includedFile : scanner.getIncludedFiles()) {
            Path file = deleteDir.resolve(includedFile);
            if (exists(file)) {
                try {
                    delete(file);
                } catch (IOException e) {
                    getLog().error("Unable to delete file", e);
                }
            }
        }
    }
}
