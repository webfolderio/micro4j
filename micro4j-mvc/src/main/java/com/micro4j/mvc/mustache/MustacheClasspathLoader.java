package com.micro4j.mvc.mustache;

import java.io.Reader;
import java.io.StringReader;

import com.micro4j.mvc.Configuration;
import com.micro4j.mvc.template.ClasspathLoader;
import com.samskivert.mustache.Mustache.TemplateLoader;

public class MustacheClasspathLoader extends ClasspathLoader implements TemplateLoader {

    public MustacheClasspathLoader(Configuration configuration) {
        super(configuration);
    }

    @Override
    public Reader getTemplate(String name) throws Exception {
        String content = get(name);
        return new StringReader(content);
    }
}
