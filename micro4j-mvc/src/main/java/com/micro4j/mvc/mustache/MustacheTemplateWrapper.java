package com.micro4j.mvc.mustache;

import java.io.Writer;

import com.micro4j.mvc.template.TemplateWrapper;
import com.samskivert.mustache.Template;

public class MustacheTemplateWrapper implements TemplateWrapper {

    private Template template;

    public MustacheTemplateWrapper(Template template) {
        this.template = template;
    }

    @Override
    public void execute(Object context, Object parentContext, Writer writer) {
        template.execute(context, parentContext, writer);
    }

    public Template getTemplate() {
        return template;
    }
}
