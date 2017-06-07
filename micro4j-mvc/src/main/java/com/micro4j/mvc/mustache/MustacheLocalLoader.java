package com.micro4j.mvc.mustache;

import java.io.Reader;
import java.io.StringReader;
import java.nio.file.Path;
import java.util.Set;

import com.micro4j.mvc.Configuration;
import com.micro4j.mvc.template.LocalLoader;
import com.samskivert.mustache.Mustache.TemplateLoader;

public class MustacheLocalLoader extends LocalLoader implements TemplateLoader {

    public MustacheLocalLoader(Configuration configuration, Set<Path> paths) {
        super(configuration, paths);
    }

    @Override
    public Reader getTemplate(String name) throws Exception {
        String content = get(name);
        return new StringReader(content);
    }
}
