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
package com.micro4j.maven.plugin;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.exists;
import static java.nio.file.Files.readAllBytes;
import static java.nio.file.Paths.get;
import static java.util.Base64.getEncoder;
import static org.apache.maven.plugins.annotations.LifecyclePhase.PREPARE_PACKAGE;

import java.io.IOException;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.sonatype.plexus.build.incremental.BuildContext;

import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Template;

import net.htmlparser.jericho.Config;
import net.htmlparser.jericho.Element;
import net.htmlparser.jericho.LoggerProvider;
import net.htmlparser.jericho.OutputDocument;
import net.htmlparser.jericho.Source;

@Mojo(name = "js-integrity", defaultPhase = PREPARE_PACKAGE, threadSafe = true, requiresOnline = false, requiresReports = false)
public class JsIntegrityMojo extends BaseMojo {

    @Parameter(defaultValue = "**/*.html")
    private String[] jsIntegrityIncludes = new String[] { "**/*.html" };

    @Parameter
    private String[] jsIntegrityExcludes;

    @Parameter(defaultValue = "${project.build.sourceEncoding}")
    private String jsIntegrityEncoding;

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
        return jsIntegrityEncoding;
    }

    @Override
    protected MavenProject getProject() {
        return project;
    }

    @Override
    protected String[] getIncludes() {
        return jsIntegrityIncludes;
    }

    @Override
    protected String[] getExcludes() {
        return jsIntegrityExcludes;
    }

    @Override
    protected BuildContext getBuildContext() {
        return buildContext;
    }

    @Override
    protected String transform(Path srcFile, Path targetFile, String content) throws MojoExecutionException {
        Source source = new Source(content);
        source.fullSequentialParse();
        List<Element> scripts = source.getAllElements("script");
        if (scripts.isEmpty()) {
            return null;
        }
        OutputDocument document = new OutputDocument(source);
        boolean modified = false;
        for (Element script : scripts) {
            try {
                String src = script.getAttributeValue("src");
                if (src == null || src.trim().isEmpty()) {
                    continue;
                }
                
                String timestamp = project.getProperties().getProperty("timestamp");
                Template template = Mustache.compiler().compile(src);
                Map<String, String> context = new HashMap<String, String>();
                context.put("contextPath", "");
                context.put("buildTime", timestamp);
                src = template.execute(context);

                String outputDirectory = project.getBuild().getOutputDirectory();
                Path resource = get(outputDirectory)
                                        .resolve("resource");
                Path jsFile = resource.resolve(src);
                if ( ! exists(jsFile) ) {
                    continue;
                }
                String jsContent = null;
                try {
                    jsContent = new String(readAllBytes(jsFile), UTF_8);
                } catch (IOException e) {
                    getLog().warn(e.getMessage(), e);
                }
                MessageDigest digest = MessageDigest.getInstance("SHA-384");
                String hash = "sha384-" + new String(getEncoder()
                                                .encode(digest.digest(jsContent.getBytes(UTF_8))),
                                                UTF_8);
                Map<String, String> map = new LinkedHashMap<>();
                map.put("integrity", hash);
                map.put("crossorigin", "anonymous");
                map = script.getAttributes().populateMap(map, false);
                document.replace(script.getAttributes(), map);
                modified = true;
            } catch (NoSuchAlgorithmException e) {
                getLog().warn(e.getMessage(), e);
            }
        }
        if ( ! modified ) {
            return null;
        } else {
            return document.toString();
        }
    }

    @Override
    protected String getOutputExtension() {
        return "html";
    }

    protected Path getCacheDirectory() {
        return null;
    }
}
