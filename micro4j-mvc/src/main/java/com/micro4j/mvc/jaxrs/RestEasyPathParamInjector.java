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
