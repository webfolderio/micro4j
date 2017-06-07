package com.micro4j.mvc.template;

import java.io.Writer;

public interface TemplateWrapper {

    void execute(Object context, Object parentContext, Writer writer);
}
