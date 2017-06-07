package com.micro4j.mvc.csrf;

import static java.lang.Boolean.FALSE;
import static java.lang.Long.toHexString;
import static org.jboss.resteasy.spi.ResteasyProviderFactory.getContextDataLevelCount;

import java.security.SecureRandom;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

import com.micro4j.mvc.template.TemplateIntereceptor;
import com.micro4j.mvc.template.TemplateWrapper;

public class MustacheCsrfInterceptor extends TemplateIntereceptor {

    private final SecureRandom random = new SecureRandom();

    private final Map<String, Boolean> cache;

    @Context
    private UriInfo uriInfo;

    public MustacheCsrfInterceptor(Map<String, Boolean> cache) {
        this.cache = cache;
    }

    @Override
    public TemplateWrapper beforeExecute(String name, TemplateWrapper templateWrapper, Object context,
            Map<String, Object> parentContext) {
        boolean enable = true;
        if (getContextDataLevelCount() > 0) {
            List<Object> resources = uriInfo.getMatchedResources();
            if (resources == null || resources.isEmpty()) {
                return templateWrapper;
            }
            enable = false;
            for (int i = 0; i < resources.size(); i++) {
                Object resource = resources.get(i);
                if (resource.getClass().isAnnotationPresent(EnableCsrfFilter.class)) {
                    enable = true;
                    break;
                }
            }
        }
        if (enable) {
            String token = toHexString(random.nextLong());
            cache.put(token, FALSE);
            parentContext.put("csrf-token", token);
        }
        return templateWrapper;
    }
}
