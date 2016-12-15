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

import java.util.Set;

import org.jboss.resteasy.core.InjectorFactoryImpl;
import org.jboss.resteasy.core.ValueInjector;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.jboss.resteasy.spi.metadata.Parameter;

class RestEasyInjectorFactory extends InjectorFactoryImpl {

    private final Set<String> excludes;

    public RestEasyInjectorFactory(Set<String> excludes) {
        this.excludes = excludes;
    }

    @Override
    public ValueInjector createParameterExtractor(Parameter parameter, ResteasyProviderFactory providerFactory) {
        String name = parameter.getParamName();
        if ( name != null &&
                    ! name.trim().isEmpty() &&
                    excludes.contains(name) ) {
            return super.createParameterExtractor(parameter, providerFactory);
        }
        switch (parameter.getParamType()) {
            case HEADER_PARAM:
                return new RestEasyHeaderParamInjector(parameter.getType(), parameter.getGenericType(),
                        parameter.getAccessibleObject(), parameter.getParamName(), parameter.getDefaultValue(),
                        parameter.getAnnotations(), providerFactory);
            case PATH_PARAM:
                return new RestEasyPathParamInjector(parameter.getType(), parameter.getGenericType(),
                        parameter.getAccessibleObject(), parameter.getParamName(), parameter.getDefaultValue(),
                        parameter.isEncoded(), parameter.getAnnotations(), providerFactory);
            case COOKIE_PARAM:
                return new RestEasyParamInjector(parameter.getType(), parameter.getGenericType(),
                        parameter.getAccessibleObject(), parameter.getParamName(), parameter.getDefaultValue(),
                        parameter.getAnnotations(), providerFactory);
            case FORM_PARAM:
                return new RestEasyFormParamInjector(parameter.getType(), parameter.getGenericType(),
                        parameter.getAccessibleObject(), parameter.getParamName(), parameter.getDefaultValue(),
                        parameter.isEncoded(), parameter.getAnnotations(), providerFactory);
            case QUERY_PARAM:
                return new RestEasyQueryParamInjector(parameter.getType(), parameter.getGenericType(),
                        parameter.getAccessibleObject(), parameter.getParamName(), parameter.getDefaultValue(),
                        parameter.isEncoded(), parameter.getAnnotations(), providerFactory);
            default:
                return super.createParameterExtractor(parameter, providerFactory);
        }
    }
}
