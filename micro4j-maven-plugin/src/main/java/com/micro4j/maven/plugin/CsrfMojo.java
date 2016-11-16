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

import static java.lang.String.format;
import static java.nio.file.Files.exists;
import static java.nio.file.Files.isDirectory;
import static java.nio.file.Files.isReadable;
import static java.nio.file.Files.write;
import static java.nio.file.Paths.get;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static org.apache.maven.plugins.annotations.LifecyclePhase.PROCESS_RESOURCES;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.Scanner;
import org.sonatype.plexus.build.incremental.BuildContext;

import net.htmlparser.jericho.Config;
import net.htmlparser.jericho.Element;
import net.htmlparser.jericho.LoggerProvider;
import net.htmlparser.jericho.OutputDocument;
import net.htmlparser.jericho.Source;
import net.htmlparser.jericho.StartTag;

@Mojo(name = "csrf", defaultPhase = PROCESS_RESOURCES, threadSafe = true, requiresOnline = false, requiresReports = false)
public class CsrfMojo extends AbstractMojo {

    private static final String DATA_CSRF = "data-csrf";

    private static final String CSRF_TOKEN = "csrf-token";

    @Parameter(defaultValue = "**/*.html")
    private String[] csrfIncludes = new String[] { "**/*.html" };

    @Parameter
    private String[] csrfExcludes;

    @Parameter(defaultValue = "${project.build.sourceEncoding}")
    private String csrfEncoding;

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Component
    private BuildContext buildContext;

    public void execute() throws MojoExecutionException, MojoFailureException {
        Config.LoggerProvider = LoggerProvider.DISABLED;
        if (project.getBuild().getOutputDirectory() != null) {
            File folder = new File(project.getBuild().getOutputDirectory());
            if (isDirectory(folder.toPath())) {
                transform(folder);
            }
        }
        if (project.getBuild().getTestOutputDirectory() != null) {
            File folder = new File(project.getBuild().getTestOutputDirectory());
            if (isDirectory(folder.toPath())) {
                transform(folder);
            }
        }
    }

    protected void transform(File folder) throws MojoExecutionException, MojoFailureException {
        boolean incremental = buildContext.isIncremental();
        boolean ignoreDelta = incremental ? false : true;
        Scanner scanner = buildContext.newScanner(folder, ignoreDelta);
        scanner.setIncludes(csrfIncludes);
        if (csrfExcludes != null && csrfExcludes.length > 0) {
            scanner.setExcludes(csrfExcludes);
        }
        scanner.scan();
        for (String includedFile : scanner.getIncludedFiles()) {
            Path htmlFile = get(includedFile);
            if (exists(htmlFile) && isReadable(htmlFile)) {
                try {
                    String content = new String(Files.readAllBytes(htmlFile), csrfEncoding);
                    String modified = injectCsrfInput(content);
                    if ( modified != null && ! modified.trim().isEmpty() ) {
                        getLog().info("Adding csrf hidden input [" + htmlFile.toString() + "]");
                        write(htmlFile, content.getBytes(csrfEncoding), TRUNCATE_EXISTING);
                        getLog().info("csrf input added [" + htmlFile.toString().toString() + "]");
                    }
                } catch (IOException e) {
                    throw new MojoExecutionException(e.getMessage(), e);
                }
            }
        }
    }

    protected String injectCsrfInput(String content) {
        Source source = new Source(content);
        source.fullSequentialParse();
        List<Element> forms = source.getAllElements("form");
        if (forms.isEmpty()) {
            return null;
        }
        OutputDocument document = new OutputDocument(source);
        boolean modified = false;
        for (Element form : forms) {
            String method = form.getAttributeValue("method");
            if ( ! "post".equalsIgnoreCase(method) ) {
                continue;
            }
            String dataCsrf = form.getAttributeValue(DATA_CSRF);
            if ("disabled".equalsIgnoreCase(dataCsrf)) {
                document.remove(form.getAttributes().get(DATA_CSRF));
                modified = true;
                continue;
            }
            String newLine = source.getNewLine() != null ? source.getNewLine() : "";
            String input = format("{{#%s}}%s<input type=\"hidden\" name=\"%s\" value=\"{{this}}\" />{{/%s}}",
                    CSRF_TOKEN,
                    newLine,
                    CSRF_TOKEN,
                    CSRF_TOKEN);
            StartTag tag = form.getFirstStartTag();
            document.replace(tag.getEnd(), tag.getEnd(), input);
            modified = true;
        }
        if ( ! modified ) {
            return null;
        } else {
            return document.toString();
        }
    }
}
