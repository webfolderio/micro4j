package io.webfolder.micro4j.mvc.mustachejava;

import java.io.Reader;
import java.io.StringReader;

import com.github.mustachejava.MustacheResolver;

import io.webfolder.micro4j.mvc.Configuration;
import io.webfolder.micro4j.mvc.template.ClasspathLoader;

public class MustacheClasspathLoader extends ClasspathLoader implements MustacheResolver {

	public MustacheClasspathLoader(Configuration configuration) {
		super(configuration);
	}

	@Override
	public Reader getReader(String resourceName) {
        String content = get(resourceName);
        return new StringReader(content);
	}
}
