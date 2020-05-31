package io.webfolder.micro4j.mvc.mustachejava;

import java.io.Reader;
import java.io.StringReader;
import java.nio.file.Path;
import java.util.Set;

import com.github.mustachejava.MustacheResolver;

import io.webfolder.micro4j.mvc.Configuration;
import io.webfolder.micro4j.mvc.template.LocalLoader;

public class MustacheLocalLoader extends LocalLoader implements MustacheResolver {

	public MustacheLocalLoader(Configuration configuration, Set<Path> paths) {
		super(configuration, paths);
	}

	@Override
	public Reader getReader(String resourceName) {
        String content = get(resourceName);
        return new StringReader(content);
	}
}
