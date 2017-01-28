/* MIT License
 * 
 * Copyright (c) 2016 - 2017 http://micro4j.com
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
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
