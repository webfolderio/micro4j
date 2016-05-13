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
package com.micro4j.mvc;

import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.ext.MessageBodyWriter;

import org.jboss.resteasy.spi.InjectorFactory;
import org.jboss.resteasy.spi.PropertyInjector;
import org.jboss.resteasy.spi.ResteasyProviderFactory;

import com.micro4j.mvc.template.Processor;

class MvcResteasyFeature implements Feature {

    private ResteasyProviderFactory providerFactory;

    private Configuration configuration;

    private MessageBodyWriter<?> viewWriter;

    public MvcResteasyFeature(Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    public boolean configure(FeatureContext context) {
        providerFactory = (ResteasyProviderFactory) context.getConfiguration();
        inject();
        context.register(viewWriter);
        return true;
    }

    void inject() {
        for (Processor processor : configuration.getProcessors()) {
            inject(processor.getClass(), processor);
        }
    }

    void inject(Class<?> klass, Object object) {
        InjectorFactory injectorFactory = getInjectorFactory();
        PropertyInjector injector = injectorFactory.createPropertyInjector(klass, providerFactory);
        injector.inject(object);
    }

    InjectorFactory getInjectorFactory() {
        InjectorFactory injectorFactory = providerFactory.getInjectorFactory();
        injectorFactory = providerFactory.getInjectorFactory();
        return injectorFactory;
    }

    void setViewWriter(MessageBodyWriter<?> viewWriter) {
        this.viewWriter = viewWriter;
    }
}
