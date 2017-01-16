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

import static java.lang.Integer.parseInt;
import static java.lang.String.valueOf;
import static java.lang.Thread.currentThread;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.sonatype.plexus.build.incremental.BuildContext.SEVERITY_ERROR;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Path;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.apache.maven.plugin.MojoExecutionException;
import org.sonatype.plexus.build.incremental.BuildContext;

class NashornEngine implements JsEngine {

    private String babelLocation;

    private String babelPresets;

    private Invocable invocable;

    public NashornEngine(String babelLocation, String babelPresets) {
        this.babelLocation = babelLocation;
        this.babelPresets = babelPresets;
    }

    @Override
    public void init() throws MojoExecutionException {
        try {
            URL url = currentThread().getContextClassLoader().getResource(babelLocation);
            if (url == null) {
                throw new MojoExecutionException("Unable to load babel from [" + babelLocation + "]");
            }
            try (InputStream is = new BufferedInputStream(url.openStream())) {
                ScriptEngineManager manager = new ScriptEngineManager(null);
                ScriptEngine scriptEngine = manager.getEngineByExtension("js");
                if (scriptEngine == null) {
                    throw new MojoExecutionException("Unable to instantiate JavaScript engine");
                }
                scriptEngine.eval(new InputStreamReader(is, UTF_8.name()));
                scriptEngine.eval("var micro4jCompile = function(input) { try { return Babel.transform(input, { presets: "
                                + babelPresets + " }).code; } catch(e) { return e;} }");
                invocable =  (Invocable) scriptEngine;
            } catch (ScriptException e) {
                throw new MojoExecutionException(e.getMessage(), e);
            }
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    @Override
    public boolean isInitialized() {
        return invocable != null ? true : false;
    }

    @Override
    public void dispose() throws MojoExecutionException {
        invocable = null;
    }

    @Override
    public String execute(Path srcFile, String content, BuildContext buildContext) throws MojoExecutionException {
        String modifiedContent;
        try {
            modifiedContent = valueOf(invocable.invokeFunction("micro4jCompile", content));
        } catch (NoSuchMethodException | ScriptException e) {
            buildContext.addMessage(srcFile.toFile(), 0, 0, e.getMessage(), SEVERITY_ERROR, null);
            throw new MojoExecutionException(e.getMessage(), e);
        }
        if (modifiedContent.startsWith("SyntaxError")) {
            int begin = modifiedContent.indexOf("(");
            int end = modifiedContent.indexOf(")");
            if (begin >= 0 && end > begin) {
                String[] position = modifiedContent.substring(begin + 1, end).split(":");
                int line = parseInt(position[0]);
                int col = parseInt(position[1]);
                buildContext.addMessage(srcFile.toFile(), line, col, modifiedContent, SEVERITY_ERROR, null);
            } else {
                throw new MojoExecutionException("Please fix babel compilation errors");
            }
        }
        return modifiedContent;
    }
}
