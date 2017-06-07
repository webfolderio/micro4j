package com.micro4j.mvc.jaxrs;

import static com.micro4j.mvc.MvcMessages.getString;
import static org.slf4j.LoggerFactory.getLogger;

import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.ext.MessageBodyWriter;

import org.slf4j.Logger;

import com.micro4j.mvc.Configuration;
import com.micro4j.mvc.Configuration.Builder;
import com.micro4j.mvc.mustache.MustacheTemplateEngine;
import com.micro4j.mvc.template.TemplateEngine;

public class MvcFeature implements Feature {

    private static final Logger LOG = getLogger(MvcFeature.class);

    private Configuration configuration;

    private TemplateEngine templateEngine;

    public MvcFeature() {
        this(new Builder().build());
    }

    public MvcFeature(Configuration configuration) {
        this(configuration, new MustacheTemplateEngine(configuration));
    }

    public MvcFeature(Configuration configuration, TemplateEngine templateEngine) {
        this.configuration = configuration;
        this.templateEngine = templateEngine;
    }

    @Override
    public boolean configure(FeatureContext context) {
        boolean initialized = false;
        if (isResteasy(context)) {
            ResteasyFeature resteasyFeature = new ResteasyFeature(configuration);
            resteasyFeature.setViewWriter(createViewWriter(templateEngine));
            initialized = resteasyFeature.configure(context);
        } else {
            LOG.warn(getString("MvcFeature.initialization.failed"), // $NON-NLS-1$
                    new Object[] { MvcFeature.class.getSimpleName() });
        }
        if (initialized) {
            LOG.info(getString("MvcFeature.initialization.success"), new Object[] { MvcFeature.class.getSimpleName() }); // $NON-NLS-1$
        }
        return initialized;
    }

    protected boolean isResteasy(FeatureContext context) {
        return "ResteasyProviderFactory".equals(context.getConfiguration().getClass().getSimpleName());
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public TemplateEngine getTemplateEngine() {
        return templateEngine;
    }

    protected MessageBodyWriter<?> createViewWriter(TemplateEngine templateEngine) {
        return new ViewWriter(templateEngine);
    }
}
