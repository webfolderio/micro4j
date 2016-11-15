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
import static org.attoparser.config.ParseConfiguration.htmlConfiguration;
import static org.attoparser.minimize.MinimizeHtmlMarkupHandler.MinimizeMode.COMPLETE;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.attoparser.MarkupParser;
import org.attoparser.ParseException;
import org.attoparser.config.ParseConfiguration;
import org.attoparser.minimize.MinimizeHtmlMarkupHandler;
import org.attoparser.minimize.MinimizeHtmlMarkupHandler.MinimizeMode;
import org.attoparser.output.OutputMarkupHandler;
import org.codehaus.plexus.compiler.util.scan.InclusionScanException;
import org.codehaus.plexus.compiler.util.scan.SimpleSourceInclusionScanner;
import org.codehaus.plexus.compiler.util.scan.SourceInclusionScanner;
import org.codehaus.plexus.compiler.util.scan.mapping.SuffixMapping;

import net.htmlparser.jericho.Element;
import net.htmlparser.jericho.OutputDocument;
import net.htmlparser.jericho.Source;
import net.htmlparser.jericho.StartTag;

@Mojo(name = "html", defaultPhase = PROCESS_RESOURCES, threadSafe = true, requiresOnline = false, requiresReports = false)
public class HtmlMojo extends AbstractMojo {

    private static final String DATA_CSRF = "data-csrf";

    private static final String CSRF_TOKEN = "csrf-token";

    @Parameter(defaultValue = "src/main/resources")
    private File resources;

    @Parameter(defaultValue = "target/classes")
    private File outputDirectory;

    @Parameter(defaultValue = "html")
    private String extension;

    @Parameter
    private String[] includes;

    @Parameter
    private String[] excludes;

    @Parameter(defaultValue = "${project.build.sourceEncoding}")
    private String encoding;

    @SuppressWarnings("unchecked")
    public void execute() throws MojoExecutionException {
        Set<String> setIncludes = includes != null ? new HashSet<>(asList(includes)) : singleton("**/*." + extension);
        Set<String> setExcludes = excludes != null ? new HashSet<>(asList(excludes)) : EMPTY_SET;
        SourceInclusionScanner scanner = new SimpleSourceInclusionScanner(setIncludes, setExcludes);
        scanner.addSourceMapping(new SuffixMapping("." + extension, "." + extension));
        try {
            Set<File> files = scanner.getIncludedSources(resources, outputDirectory);
            for (File file : files) {
                Path relativePath = resources.toPath().relativize(file.toPath());
                Path outputPath = outputDirectory.toPath().resolve(relativePath);
                StringWriter writer = new StringWriter();
                OutputMarkupHandler outputHandler = new OutputMarkupHandler(writer);
                MinimizeHtmlMarkupHandler minimizeHandler = new MinimizeHtmlMarkupHandler(getMinimizeMode(),
                        outputHandler);
                MarkupParser parser = new MarkupParser(getParseConfiguration());
                String content = new String(readAllBytes(file.toPath()), encoding);
                try {
                    String csrfOutput = injectCsrfInput(content);
                    if (csrfOutput != null) {                        
                        content = csrfOutput;
                        getLog().info("Added csrf form parameter content [" + relativePath.toString() + "]");
                    }
                    getLog().info("Minimizing html content [" + relativePath.toString() + "]");
                    parser.parse(content, minimizeHandler);
                    getLog().info("html content minimized to [" + outputPath.toString().toString() + "]");
                } catch (ParseException e) {
                    throw new MojoExecutionException(e.getMessage(), e);
                }
                write(outputPath, writer.toString().getBytes(encoding));
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

    protected ParseConfiguration getParseConfiguration() {
        return htmlConfiguration();
    }

    protected MinimizeMode getMinimizeMode() {
        return COMPLETE;
    }
}
