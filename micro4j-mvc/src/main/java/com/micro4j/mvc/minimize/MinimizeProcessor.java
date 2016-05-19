package com.micro4j.mvc.minimize;

import static org.attoparser.config.ParseConfiguration.htmlConfiguration;
import static org.attoparser.minimize.MinimizeHtmlMarkupHandler.MinimizeMode.COMPLETE;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.StringWriter;

import org.attoparser.MarkupParser;
import org.attoparser.ParseException;
import org.attoparser.config.ParseConfiguration;
import org.attoparser.minimize.MinimizeHtmlMarkupHandler;
import org.attoparser.minimize.MinimizeHtmlMarkupHandler.MinimizeMode;
import org.attoparser.output.OutputMarkupHandler;
import org.slf4j.Logger;

import com.micro4j.mvc.template.Processor;

import static com.micro4j.mvc.message.MvcMessages.getString;

public class MinimizeProcessor extends Processor {

    private static final Logger LOG = getLogger(MinimizeProcessor.class);

    public String beforeCompile(String name, String content) {
        StringWriter writer = new StringWriter();
        OutputMarkupHandler outputHandler = new OutputMarkupHandler(writer);
        MinimizeHtmlMarkupHandler minimizeHandler = new MinimizeHtmlMarkupHandler(getMinimizeMode(), outputHandler);
        MarkupParser parser = new MarkupParser(getParseConfiguration());
        try {
            parser.parse(content, minimizeHandler);
        } catch (ParseException e) {
            LOG.error(getString("MinimizeProcessor.unable.to.minimize") + " [" + name + "]", e);
        }
        return writer.toString();
    }

    protected ParseConfiguration getParseConfiguration() {
        return htmlConfiguration();
    }

    protected MinimizeMode getMinimizeMode() {
        return COMPLETE;
    }
}
