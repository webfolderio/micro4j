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

import static com.eclipsesource.v8.V8.createV8Runtime;
import static java.lang.Integer.parseInt;
import static java.lang.String.valueOf;
import static java.lang.Thread.currentThread;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.sonatype.plexus.build.incremental.BuildContext.SEVERITY_ERROR;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;

import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.IOUtil;
import org.sonatype.plexus.build.incremental.BuildContext;

import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Array;

class V8Engine implements JsEngine {

    private String babelLocation;

    private String babelPresets;

    private V8 runtime;

    public V8Engine(String babelLocation, String babelPresets) {
        this.babelLocation = babelLocation;
        this.babelPresets = babelPresets;
    }

    @Override
    public void init() throws MojoExecutionException {
        try {
            runtime = getRuntime();
        } catch (Throwable t) {
        }
    }

    @Override
    public boolean isInitialized() {
        return runtime != null && V8.isLoaded();
    }

    @Override
    public void dispose() throws MojoExecutionException {
        if ( runtime != null && ! runtime.isReleased() ) {
            runtime.release();
        }
    }

    @Override
    public String execute(Path srcFile, String content, BuildContext buildContext) throws MojoExecutionException {
        try {
            V8Array arguments = new V8Array(runtime);
            arguments.push(content);
            String modifiedContent = valueOf(valueOf(runtime.executeFunction("micro4jCompile", arguments)));
            if ( ! arguments.isReleased() ) {
                arguments.release();
            }
            if (modifiedContent.trim().startsWith("SyntaxError")) {
                int begin = modifiedContent.indexOf("(");
                int end = modifiedContent.indexOf(")");
                if (begin >= 0 && end > begin) {
                    String[] position = modifiedContent.substring(begin + 1, end).split(":");
                    int line = parseInt(position[0]);
                    int col = parseInt(position[1]);
                    buildContext.addMessage(srcFile.toFile(), line, col, modifiedContent, SEVERITY_ERROR, null);
                    return modifiedContent;
                } else {
                    throw new MojoExecutionException("Please fix babel compilation errors");
                }
            }
            return modifiedContent;
        } catch (Throwable t) {
            throw new MojoExecutionException(t.getMessage(), t);
        }
    }

    protected V8 getRuntime() throws MojoExecutionException {
        V8 runtime = null;
        URL url = currentThread().getContextClassLoader().getResource(babelLocation);
        if (url == null) {
            throw new MojoExecutionException("Unable to load babel from [" + babelLocation + "]");
        }
        try (InputStream is = new BufferedInputStream(url.openStream())) {
            runtime = createV8Runtime();
            runtime.executeScript(IOUtil.toString(is, UTF_8.name()));
            runtime.executeScript("var micro4jCompile = function(input) { try { return Babel.transform(input, { presets: "
                    + babelPresets + " }).code; } catch(e) { return e;} }");
            return runtime;
        } catch (Throwable e) {
            if (runtime != null) {
                runtime.release();
            }
        }
        return null;
    }
}
