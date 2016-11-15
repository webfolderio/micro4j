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
package com.micro4j.mvc.csrf;

import java.lang.reflect.Method;

import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.FeatureContext;

public class CsrfFeature implements DynamicFeature {

    private ContainerResponseFilter csrFilter;

    public CsrfFeature(ContainerResponseFilter csrfFilter) {
        this.csrFilter = csrfFilter;
    }

    @Override
    public void configure(ResourceInfo resourceInfo, FeatureContext context) {
        if (ignore(resourceInfo.getResourceClass())) {
            return;
        }
        if (ignore(resourceInfo.getResourceMethod())) {
            return;
        }
        context.register(csrFilter);
    }

    protected boolean ignore(Class<?> klass) {
        return klass.isAnnotationPresent(GET.class) || klass.isAnnotationPresent(OPTIONS.class)
                || klass.isAnnotationPresent(HEAD.class);
    }

    protected boolean ignore(Method method) {
        return method.isAnnotationPresent(GET.class) || method.isAnnotationPresent(OPTIONS.class)
                || method.isAnnotationPresent(HEAD.class);
    }
}
