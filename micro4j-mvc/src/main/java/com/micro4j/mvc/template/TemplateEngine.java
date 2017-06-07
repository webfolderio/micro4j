package com.micro4j.mvc.template;

import java.io.Writer;
import java.util.Map;

import com.micro4j.mvc.Configuration;

public interface TemplateEngine {

    void execute(String name, Object context, Map<String, Object> parentContext, Writer writer);

    Configuration getConfiguration();

    ContentLoader getContentLoader();
}
