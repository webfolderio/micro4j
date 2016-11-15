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
package com.micro4j.mvc.jaxrs;

import static org.glassfish.jersey.ServiceLocatorProvider.getServiceLocator;

import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.ext.MessageBodyWriter;

import org.glassfish.hk2.api.ServiceLocator;

import com.micro4j.mvc.Configuration;
import com.micro4j.mvc.template.DefaultFormatter;
import com.micro4j.mvc.template.TemplateIntereceptor;

class JerseyFeature implements Feature {

    private ServiceLocator serviceLocator;

    private Configuration configuration;

    private MessageBodyWriter<?> viewWriter;

    JerseyFeature(Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    public boolean configure(FeatureContext context) {
        serviceLocator = getServiceLocator(context);
        inject();
        context.register(viewWriter);
        return true;
    }

    protected void inject() {
        for (TemplateIntereceptor interceptor : configuration.getInterceptors()) {
            inject(interceptor);
            interceptor.init();
        }
        DefaultFormatter formatter = configuration.getFormatter();
        if (formatter != null) {
            inject(formatter);
            if (DefaultFormatter.class.isAssignableFrom(formatter.getClass())) {
                ((DefaultFormatter) formatter).init();
            }
        }
    }

    protected void inject(Object object) {
        serviceLocator.inject(object);
    }

    protected void setViewWriter(MessageBodyWriter<?> viewWriter) {
        this.viewWriter = viewWriter;
    }
}