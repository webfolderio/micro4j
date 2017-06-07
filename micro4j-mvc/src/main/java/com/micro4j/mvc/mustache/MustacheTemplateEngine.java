package com.micro4j.mvc.mustache;

import static com.samskivert.mustache.Mustache.compiler;

import com.micro4j.mvc.Configuration;
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
                .nullValue(configuration.getNullValue())
                .withDelims(configuration.getDelims())
                .withFormatter((Formatter) configuration.getFormatter());
    }
}
