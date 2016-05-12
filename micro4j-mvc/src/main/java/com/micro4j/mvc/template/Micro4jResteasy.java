/* MIT License
 * 
 * Copyright (c) 2016 http://micro4j.com
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
package com.micro4j.mvc.template;

import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.ext.MessageBodyWriter;

import org.jboss.resteasy.spi.InjectorFactory;
import org.jboss.resteasy.spi.PropertyInjector;
import org.jboss.resteasy.spi.ResteasyDeployment;
import org.jboss.resteasy.spi.ResteasyProviderFactory;

import com.micro4j.mvc.mustache.MustacheTemplateEngine;
import com.micro4j.mvc.view.ViewWriter;

public class Micro4jResteasy implements Feature {

    private ResteasyProviderFactory providerFactory;

    private ResteasyDeployment deployment;

    private Configuration configuration;

    private TemplateEngine templateEngine;

    public Micro4jResteasy(Configuration configuration, ResteasyDeployment deployment) {
        this(configuration);
        this.deployment = deployment;
    }

    public Micro4jResteasy(Configuration configuration, ResteasyDeployment deployment, TemplateEngine templateEngine) {
        this(configuration, templateEngine);
        this.deployment = deployment;
    }

    public Micro4jResteasy(Configuration configuration, ResteasyProviderFactory providerFactory) {
        this(configuration);
        this.providerFactory = providerFactory;
    }

    public Micro4jResteasy(Configuration configuration, ResteasyProviderFactory providerFactory, TemplateEngine templateEngine) {
        this(configuration, templateEngine);
        this.providerFactory = providerFactory;
    }

    public Micro4jResteasy(Configuration configuration) {
        this(configuration, new MustacheTemplateEngine(configuration));
    }

    public Micro4jResteasy(Configuration configuration, TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
        this.configuration = configuration;
    }

    @Override
    public boolean configure(FeatureContext context) {
        inject();
        context.register(createViewWriter(templateEngine));
        return true;
    }

    protected void inject() {
        for (Processor processor : configuration.getProcessors()) {
            inject(processor.getClass(), processor);
        }
    }

    protected void inject(Class<?> klass, Object object) {
        InjectorFactory injectorFactory = getInjectorFactory();
        PropertyInjector injector = injectorFactory.createPropertyInjector(klass, providerFactory);
        injector.inject(object);
    }

    protected InjectorFactory getInjectorFactory() {
        InjectorFactory injectorFactory = null;
        if (deployment != null) {
            providerFactory = deployment.getProviderFactory();
            injectorFactory = providerFactory.getInjectorFactory();
        } else if (this.providerFactory != null) {
            injectorFactory = providerFactory.getInjectorFactory();
        } else {
            providerFactory = ResteasyProviderFactory.getInstance();
            injectorFactory = providerFactory.getInjectorFactory();
        }
        return injectorFactory;
    }

    protected MessageBodyWriter<?> createViewWriter(TemplateEngine templateEngine) {
        return new ViewWriter(templateEngine);
    }
}
