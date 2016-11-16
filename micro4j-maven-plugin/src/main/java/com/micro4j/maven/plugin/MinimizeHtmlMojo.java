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

import static java.nio.file.Files.isDirectory;
import static java.nio.file.Files.readAllBytes;
import static java.nio.file.Files.write;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static org.apache.maven.plugins.annotations.LifecyclePhase.PROCESS_RESOURCES;
import static org.attoparser.config.ParseConfiguration.htmlConfiguration;
import static org.attoparser.minimize.MinimizeHtmlMarkupHandler.MinimizeMode.COMPLETE;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Path;

import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.attoparser.MarkupParser;
import org.attoparser.ParseException;
import org.attoparser.config.ParseConfiguration;
import org.attoparser.minimize.MinimizeHtmlMarkupHandler;
import org.attoparser.minimize.MinimizeHtmlMarkupHandler.MinimizeMode;
import org.attoparser.output.OutputMarkupHandler;
import org.codehaus.plexus.util.Scanner;
import org.sonatype.plexus.build.incremental.BuildContext;

@Mojo(name = "minimize-html", defaultPhase = PROCESS_RESOURCES, threadSafe = true, requiresOnline = false, requiresReports = false)
public class MinimizeHtmlMojo extends AbstractMojo {

    @Parameter(defaultValue = "**/*.html")
    private String[] minimizeHtmlIncludes = new String[] { "**/*.html" };

    @Parameter
    private String[] minimizeHtmlExcludes;

    @Parameter(defaultValue = "${project.build.sourceEncoding}")
    private String minimizeHtmlEncoding;

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Component
    private BuildContext buildContext;

    public void execute() throws MojoExecutionException {
        if (project.getResources() != null) {
            for (Resource resource : project.getResources()) {
                File srcDir = new File(resource.getDirectory());
                if (isDirectory(srcDir.toPath())) {
                    transform(srcDir, project.getBuild().getOutputDirectory());
                }
            }
        }
        if (project.getTestResources() != null) {
            for (Resource resource : project.getTestResources()) {
                File srcDir = new File(resource.getDirectory());
                if (isDirectory(srcDir.toPath())) {
                    transform(srcDir, project.getBuild().getTestOutputDirectory());
                }
            }
        }
    }

    protected void transform(File srcDir, String destDir) throws MojoExecutionException {
        boolean incremental = buildContext.isIncremental();
        boolean ignoreDelta = incremental ? false : true;
        Scanner scanner = buildContext.newScanner(srcDir, ignoreDelta);
        scanner.setIncludes(minimizeHtmlIncludes);
        if (minimizeHtmlExcludes != null && minimizeHtmlExcludes.length > 0) {
            scanner.setExcludes(minimizeHtmlExcludes);
        }
        scanner.scan();
        for (String includedFile : scanner.getIncludedFiles()) {
            Path srcFile = srcDir.toPath().resolve(includedFile);
            Path baseDir = scanner.getBasedir().toPath();
            Path minFile = new File(destDir).toPath().resolve(baseDir.relativize(srcFile));
            boolean isUptodate = buildContext.isUptodate(minFile.toFile(), srcFile.toFile());
            if ( ! isUptodate ) {
                try {
                    Path relativePath = new File(project.getBuild().getSourceDirectory()).toPath().relativize(srcFile);
                    Path outputPath = new File(project.getBuild().getOutputDirectory()).toPath().resolve(relativePath);
                    StringWriter writer = new StringWriter();
                    OutputMarkupHandler outputHandler = new OutputMarkupHandler(writer);
                    MinimizeHtmlMarkupHandler minimizeHandler = new MinimizeHtmlMarkupHandler(getMinimizeMode(),
                            outputHandler);
                    MarkupParser parser = new MarkupParser(getParseConfiguration());
                    String content = new String(readAllBytes(srcFile), minimizeHtmlEncoding);
                    getLog().info("Minimizing html content [" + relativePath.toString() + "]");
                    parser.parse(content, minimizeHandler);
                    getLog().info("html content minimized to [" + outputPath.toString().toString() + "]");
                    write(outputPath, writer.toString().getBytes(minimizeHtmlEncoding), TRUNCATE_EXISTING);
                } catch (IOException e) {
                    throw new MojoExecutionException(e.getMessage(), e);
                } catch (ParseException e) {
                    throw new MojoExecutionException(e.getMessage(), e);
                }
            }
        }
    }

    protected ParseConfiguration getParseConfiguration() {
        return htmlConfiguration();
    }

    protected MinimizeMode getMinimizeMode() {
        return COMPLETE;
    }
}
