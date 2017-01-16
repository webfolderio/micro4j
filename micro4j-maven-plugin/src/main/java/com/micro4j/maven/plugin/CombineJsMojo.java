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

import static java.util.regex.Pattern.DOTALL;
import static java.util.regex.Pattern.compile;
import static org.apache.maven.plugins.annotations.LifecyclePhase.PREPARE_PACKAGE;

import java.nio.file.Path;
import java.util.regex.Pattern;

import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.sonatype.plexus.build.incremental.BuildContext;

@Mojo(name = "combine-js", defaultPhase = PREPARE_PACKAGE, threadSafe = true, requiresOnline = false, requiresReports = false)
public class CombineJsMojo extends AbstractCombineMojo {

    @Parameter(defaultValue = "**/*-combined.js, **/*-combined.min.js")
    private String[] combineJsIncludes = new String[] { "**/*-combined.js", "**/*-combined.min.js" };

    @Parameter
    private String[] combineJsExcludes;

    @Parameter(defaultValue = "js")
    private String combineJsOutputExtension = "js";

    @Parameter(defaultValue = "${project.build.sourceEncoding}")
    private String combineJsEncoding;

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Component
    private BuildContext buildContext;

    private static final Pattern[] REQUIRE_PATTERNS = new Pattern[] {
            compile("\\s*var\\s+.*\\s*=\\s*require\\(['|\"](?<path>.*?)['|\"]\\)", DOTALL)
    };

    @Override
    protected void init() {
    }

    @Override
    protected String getEncoding() {
        return combineJsEncoding;
    }

    @Override
    protected MavenProject getProject() {
        return project;
    }

    @Override
    protected String[] getIncludes() {
        return combineJsIncludes;
    }

    @Override
    protected String[] getExcludes() {
        return combineJsExcludes;
    }

    @Override
    protected BuildContext getBuildContext() {
        return buildContext;
    }

    @Override
    protected String getOutputExtension() {
        return combineJsOutputExtension;
    }

    protected Path getCacheDirectory() {
        return null;
    }

    @Override
    protected Pattern[] getPatterns() {
        return REQUIRE_PATTERNS;
    }
}
