package com.micro4j.mvc.mustache;

import static com.micro4j.mvc.MvcMessages.getString;
import static com.samskivert.mustache.Mustache.compiler;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;

import com.samskivert.mustache.Mustache.Compiler;
import com.samskivert.mustache.Mustache.Lambda;
import com.samskivert.mustache.Template;
import com.samskivert.mustache.Template.Fragment;

public class MustacheI18nLambda implements Lambda {

    private final Map<String, Template> templates = new ConcurrentHashMap<>();

    private static final Logger LOG = getLogger(MustacheI18nLambda.class);

    public MustacheI18nLambda(ResourceBundle bundle) {
        Compiler compiler = compiler().escapeHTML(false);
        for (String key : bundle.keySet()) {
            String value = bundle.getString(key);
            Template template = compiler.compile(value);
            templates.put(key, template);
        }
    }

    @Override
    public void execute(Fragment frag, Writer out) throws IOException {
        String key = frag.execute().trim();
        Template template = templates.get(key);
        if (template == null) {
            LOG.error(getString("MustacheI18nLambda.key.not.found"), new Object[] { key });
            out.write(key);            
        } else {
            Object parentContext = frag.context(1);
            StringWriter writer = new StringWriter();
            template.execute(frag.context(), parentContext, writer);
            out.write(writer.toString());
        }
    }
}
