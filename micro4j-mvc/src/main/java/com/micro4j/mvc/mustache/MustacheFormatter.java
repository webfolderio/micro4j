package com.micro4j.mvc.mustache;

import com.micro4j.mvc.template.DefaultFormatter;
import com.samskivert.mustache.Mustache.Formatter;

public class MustacheFormatter extends DefaultFormatter implements Formatter {

    @Override
    public String format(Object value) {
        return toString(value);
    }
}
