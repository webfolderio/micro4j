package com.micro4j.mvc.jaxrs;

import static com.micro4j.mvc.jaxrs.Escapers.escape;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Type;

import org.jboss.resteasy.core.CookieParamInjector;
import org.jboss.resteasy.spi.ResteasyProviderFactory;

class RestEasyParamInjector extends CookieParamInjector {

    @SuppressWarnings("rawtypes")
    public RestEasyParamInjector(Class type, Type genericType, AccessibleObject target, String cookieName,
            String defaultValue, Annotation[] annotations, ResteasyProviderFactory factory) {
        super(type, genericType, target, cookieName, defaultValue, annotations, factory);
    }

    @Override
    public Object extractValue(String strVal) {
        String str = escape(strVal);
        return super.extractValue(str);
    }
}
