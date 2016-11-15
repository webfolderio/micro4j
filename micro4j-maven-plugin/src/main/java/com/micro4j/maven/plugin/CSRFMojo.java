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
import static java.nio.file.Files.readAllBytes;
import static java.nio.file.Files.write;
import static java.util.Arrays.asList;
import static java.util.Collections.EMPTY_SET;
import static java.util.Collections.singleton;
import static org.apache.maven.plugins.annotations.LifecyclePhase.PROCESS_RESOURCES;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.compiler.util.scan.InclusionScanException;
import org.codehaus.plexus.compiler.util.scan.SimpleSourceInclusionScanner;
import org.codehaus.plexus.compiler.util.scan.SourceInclusionScanner;
import org.codehaus.plexus.compiler.util.scan.mapping.SuffixMapping;

import net.htmlparser.jericho.Config;
import net.htmlparser.jericho.Element;
import net.htmlparser.jericho.LoggerProvider;
import net.htmlparser.jericho.OutputDocument;
import net.htmlparser.jericho.Source;
import net.htmlparser.jericho.StartTag;

@Mojo(name = "csrf", defaultPhase = PROCESS_RESOURCES, threadSafe = true, requiresOnline = false, requiresReports = false)
public class CSRFMojo extends AbstractMojo {

    private static final String DATA_CSRF = "data-csrf";

    private static final String CSRF_TOKEN = "csrf-token";

    @Parameter(defaultValue = "target/classes")
    private File csrfResources;

    @Parameter(defaultValue = "target/classes")
    private File csrfOutputDirectory;

    @Parameter(defaultValue = "html")
    private String csrfExtension;

    @Parameter
    private String[] csrfIncludes;

    @Parameter
    private String[] csrfExcludes;

    @Parameter(defaultValue = "${project.build.sourceEncoding}")
    private String encoding;

    @Override
    @SuppressWarnings("unchecked")
    public void execute() throws MojoExecutionException, MojoFailureException {
        Config.LoggerProvider = LoggerProvider.DISABLED;
        Set<String> setIncludes = csrfIncludes != null ? new HashSet<>(asList(csrfIncludes)) : singleton("**/*." + csrfExtension);
        Set<String> setExcludes = csrfExcludes != null ? new HashSet<>(asList(csrfExcludes)) : EMPTY_SET;
        SourceInclusionScanner scanner = new SimpleSourceInclusionScanner(setIncludes, setExcludes);
        if ( ! csrfResources.equals(csrfOutputDirectory) ) {
            scanner.addSourceMapping(new SuffixMapping("." + csrfExtension, "." + csrfExtension));
        }
        try {
            Set<File> files = scanner.getIncludedSources(csrfResources, csrfOutputDirectory);
            for (File file : files) {
                Path relativePath = csrfResources.toPath().relativize(file.toPath());
                Path outputPath = csrfOutputDirectory.toPath().resolve(relativePath);
                String content = new String(readAllBytes(file.toPath()), encoding);
                String modifiedContent = injectCsrfInput(content);
                if ( modifiedContent != null && ! modifiedContent.trim().isEmpty() ) {
                    getLog().info("Adding csrf input [" + relativePath.toString() + "]");
                    write(outputPath, modifiedContent.getBytes(encoding));
                    getLog().info("csrf input added [" + outputPath.toString().toString() + "]");
                }
            }
        } catch (InclusionScanException | IOException e) {
            throw new MojoExecutionException(e.getMessage());
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
            String input = format("%s<input type=\"hidden\" name=\"%s\" value=\"{{%s}}\" />", source.getNewLine(), CSRF_TOKEN,
                    CSRF_TOKEN, CSRF_TOKEN);
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

