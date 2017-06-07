package com.micro4j.mvc.jaxrs;

import static com.micro4j.mvc.jaxrs.Escapers.escape;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Type;

import org.jboss.resteasy.core.QueryParamInjector;
import org.jboss.resteasy.spi.ResteasyProviderFactory;

class RestEasyQueryParamInjector extends QueryParamInjector {

    @SuppressWarnings("rawtypes")
    public RestEasyQueryParamInjector(Class type, Type genericType, AccessibleObject target, String paramName,
            String defaultValue, boolean encode, Annotation[] annotations, ResteasyProviderFactory factory) {
        super(type, genericType, target, paramName, defaultValue, encode, annotations, factory);
    }

    @Override
    public Object extractValue(String strVal) {
        String str = escape(strVal);
        return super.extractValue(str);
    }
}
