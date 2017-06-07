package com.micro4j.mvc.jaxrs;

import static com.micro4j.mvc.jaxrs.Escapers.escape;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Type;

import org.jboss.resteasy.core.FormParamInjector;
import org.jboss.resteasy.spi.ResteasyProviderFactory;

class RestEasyFormParamInjector extends FormParamInjector {

    @SuppressWarnings("rawtypes")
    public RestEasyFormParamInjector(Class type, Type genericType, AccessibleObject target, String header,
            String defaultValue, boolean encode, Annotation[] annotations, ResteasyProviderFactory factory) {
        super(type, genericType, target, header, defaultValue, encode, annotations, factory);
    }

    @Override
    public Object extractValue(String strVal) {
        String str = escape(strVal);
        return super.extractValue(str);
    }
}
