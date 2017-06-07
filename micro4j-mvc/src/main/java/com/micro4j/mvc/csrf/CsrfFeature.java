package com.micro4j.mvc.csrf;

import java.lang.reflect.Method;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.FeatureContext;

import static java.util.Collections.emptyMap;

public class CsrfFeature implements DynamicFeature {

    private ContainerRequestFilter csrFilter;

    private Map<String, Boolean> cache = emptyMap();

    public CsrfFeature(Map<String, Boolean> cache) {
        this.cache = cache;
    }

    @Override
    public void configure(ResourceInfo resourceInfo, FeatureContext context) {
        if (isResteasy(context)) {
            csrFilter = new RestEasyCsrfFilter(cache);
        } if (ignore(resourceInfo.getResourceClass())) {
            return;
        }
        if (ignore(resourceInfo.getResourceMethod())) {
            return;
        }
        if (resourceInfo.getResourceClass().isAnnotationPresent(EnableCsrfFilter.class)) {
            context.register(csrFilter);
        }
    }

    protected boolean isResteasy(FeatureContext context) {
        return "ResteasyProviderFactory".equals(context.getConfiguration().getClass().getSimpleName());
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
