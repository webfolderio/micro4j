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
package com.micro4j.mvc.mustache;

import static com.samskivert.mustache.Mustache.compiler;

import com.micro4j.mvc.api.Configuration;
import com.micro4j.mvc.template.AbstractTemplateEngine;
import com.micro4j.mvc.template.ContentLoader;
import com.micro4j.mvc.template.TemplateWrapper;
import com.samskivert.mustache.Mustache.Compiler;
import com.samskivert.mustache.Mustache.Formatter;
import com.samskivert.mustache.Mustache.TemplateLoader;
import com.samskivert.mustache.Template;

public class MustacheTemplateEngine extends AbstractTemplateEngine {

    private Compiler compiler;

    public MustacheTemplateEngine(Configuration configuration) {
        this(new MustacheClasspathLoader(configuration), configuration);
    }

    public MustacheTemplateEngine(ContentLoader contentLoader, Configuration configuration) {
        super(configuration, contentLoader);
        compiler = createCompiler(contentLoader, configuration);
    }

    @Override
    public TemplateWrapper compile(String name, String content) {
        Template template = compiler.compile(content);
        TemplateWrapper wrapper = new MustacheTemplateWrapper(template);
        return wrapper;
    }

    protected Compiler createCompiler(ContentLoader contentLoader, Configuration configuration) {
        return compiler()
                .withLoader((TemplateLoader) contentLoader)
                .nullValue(configuration.defaultNullValue())
                .withFormatter((Formatter) configuration.getFormatter());
    }
}
