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
package com.micro4j.mvc.jaxrs;

import static com.micro4j.mvc.jaxrs.Escapers.escape;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Type;

import org.jboss.resteasy.core.PathParamInjector;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.HttpResponse;
import org.jboss.resteasy.spi.ResteasyProviderFactory;

class RestEasyPathParamInjector extends PathParamInjector {

    @SuppressWarnings("rawtypes")
    public RestEasyPathParamInjector(Class type, Type genericType, AccessibleObject target, String paramName,
            String defaultValue, boolean encode, Annotation[] annotations, ResteasyProviderFactory factory) {
        super(type, genericType, target, paramName, defaultValue, encode, annotations, factory);
    }

    @Override
    public Object inject(HttpRequest request, HttpResponse response) {
        Object value = super.inject(request, response);
        if (value != null && value instanceof String) {
            return escape((String) value);
        }
        return value;
    }   
}
