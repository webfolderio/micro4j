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
import static org.apache.maven.plugins.annotations.LifecyclePhase.PROCESS_RESOURCES;

import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.sonatype.plexus.build.incremental.BuildContext;

import net.htmlparser.jericho.Config;
import net.htmlparser.jericho.Element;
import net.htmlparser.jericho.LoggerProvider;
import net.htmlparser.jericho.OutputDocument;
import net.htmlparser.jericho.Source;
import net.htmlparser.jericho.StartTag;

@Mojo(name = "csrf", defaultPhase = PROCESS_RESOURCES, threadSafe = true, requiresOnline = false, requiresReports = false)
public class CsrfMojo extends BaseMojo {

    private static final String DATA_CSRF = "data-csrf";

    private static final String CSRF_TOKEN = "csrf-token";

    @Parameter(defaultValue = "**/*.html")
    private String[] csrfIncludes = new String[] { "**/*.html" };

    @Parameter
    private String[] csrfExcludes;

    @Parameter(defaultValue = "html")
    private String csrfOutputExtension = "html";

    @Parameter(defaultValue = "${project.build.sourceEncoding}")
    private String csrfEncoding;

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Component
    private BuildContext buildContext;

    @Override
    protected void init() {
        Config.LoggerProvider = LoggerProvider.DISABLED;
    }

    @Override
    protected String getEncoding() {
        return csrfEncoding;
    }

    @Override
    protected MavenProject getProject() {
        return project;
    }

    @Override
    protected String[] getIncludes() {
        return csrfIncludes;
    }

    @Override
    protected String[] getExcludes() {
        return csrfExcludes;
    }

    @Override
    protected BuildContext getBuildContext() {
        return buildContext;
    }

    @Override
    protected String getOutputExtension() {
        return csrfOutputExtension;
    }

    @Override
    protected String transform(String content) throws MojoExecutionException {
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
