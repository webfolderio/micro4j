
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
}
