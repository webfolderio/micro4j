/**
 * The MIT License
 * Copyright © 2016 - 2020 WebFolder OÜ
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
package io.webfolder.micro4j.mvc.mustachejava;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.github.mustachejava.MustacheResolver;

import io.webfolder.micro4j.mvc.Configuration;
import io.webfolder.micro4j.mvc.template.AbstractTemplateEngine;
import io.webfolder.micro4j.mvc.template.ContentLoader;
import io.webfolder.micro4j.mvc.template.TemplateWrapper;

public class MustacheTemplateEngine extends AbstractTemplateEngine {

	private MustacheFactory compiler;

	public MustacheTemplateEngine(Configuration configuration) {
		this(new MustacheClasspathLoader(configuration), configuration);
	}

    public MustacheTemplateEngine(ContentLoader contentLoader, Configuration configuration) {
        super(configuration, contentLoader);
        compiler = createCompiler(contentLoader, configuration);
    }

	@Override	
	public TemplateWrapper compile(String name, String content) {	    
        Mustache template = compiler.compile(name);
        MustacheTemplateWrapper wrapper = new MustacheTemplateWrapper(template);
		return wrapper;
	}

    protected MustacheFactory createCompiler(ContentLoader contentLoader, Configuration configuration) {        
        MustacheFactory compiler = new DefaultMustacheFactory((MustacheResolver) contentLoader);
        return compiler;
    }

    @Override
    public Object createFunction(String content) {
        return new MustacheContentLamabda(content);
    }
}
