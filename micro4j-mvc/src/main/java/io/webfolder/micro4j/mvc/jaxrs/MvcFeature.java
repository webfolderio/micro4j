/**
 * The MIT License
 * Copyright © 2016 - 2017 WebFolder OÜ
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package io.webfolder.micro4j.mvc.jaxrs;

import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.ext.MessageBodyWriter;

import io.webfolder.micro4j.mvc.Configuration;
import io.webfolder.micro4j.mvc.Configuration.Builder;
import io.webfolder.micro4j.mvc.mustache.MustacheTemplateEngine;
import io.webfolder.micro4j.mvc.template.TemplateEngine;

public class MvcFeature implements Feature {

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
        ResteasyFeature resteasyFeature = new ResteasyFeature(configuration);
        resteasyFeature.setViewWriter(createViewWriter(templateEngine));
        resteasyFeature.configure(context);
        return true;
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
