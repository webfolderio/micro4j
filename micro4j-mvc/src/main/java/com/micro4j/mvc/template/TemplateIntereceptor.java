package com.micro4j.mvc.template;

import java.util.Map;

import com.micro4j.mvc.Configuration;

public class TemplateIntereceptor {

    protected Configuration configuration;

    public void init() {
    }

    public String beforeCompile(String name, String content) {
        return content;
    }

    public TemplateWrapper afterCompile(TemplateWrapper templateWrapper) {
        return templateWrapper;
    }

    public TemplateWrapper beforeExecute(String name, TemplateWrapper templateWrapper, Object context, Map<String, Object> parentContext) {
        return templateWrapper;
    }

    public String afterExecute(String name, String content, Object context, Map<String, Object> parentContext) {
        return content;
    }

    public void setConfiguration(Configuration configuration) {
        if (this.configuration == null) {
            this.configuration = configuration;
        } else {
            throw new IllegalStateException();
        }
    }
}
