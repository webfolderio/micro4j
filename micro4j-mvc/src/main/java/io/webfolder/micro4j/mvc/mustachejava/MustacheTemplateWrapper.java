package io.webfolder.micro4j.mvc.mustachejava;

import static java.util.Arrays.asList;

import java.io.Writer;

import com.github.mustachejava.Mustache;

import io.webfolder.micro4j.mvc.template.TemplateWrapper;

public class MustacheTemplateWrapper implements TemplateWrapper {

    private final Mustache mustache;

	public MustacheTemplateWrapper(Mustache mustache) {
        this.mustache = mustache;
	}

	@Override
	public void execute(Object context, Object parentContext, Writer writer) {
		mustache.execute(writer, asList(context, parentContext));
	}
}
