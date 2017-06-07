package com.micro4j.mvc.csrf;

import static java.lang.Boolean.TRUE;

import java.io.IOException;
import java.util.Map;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.MultivaluedMap;

public abstract class AbstractCsrfFilter implements ContainerRequestFilter {

    protected static final String CSRF_TOKEN = "csrf-token";

    private final Map<String, Boolean> cache;

    public AbstractCsrfFilter(Map<String, Boolean> cache) {
        this.cache = cache;
    }

    protected abstract MultivaluedMap<String, String> getFormParameters(ContainerRequestContext requestContext);

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        if ( ! requestContext.hasEntity() ) {
            return;
        }
        MultivaluedMap<String, String> parameters = getFormParameters(requestContext);
        if (parameters == null) {
            return;
        }
        String token = parameters.getFirst(CSRF_TOKEN);
        if (token == null || token.trim().isEmpty()) {
            throw new InvalidCsrfTokenException();
        }
        Boolean expired = cache.get(token);
        if ( expired == null || TRUE.equals(expired) ) {
            throw new InvalidCsrfTokenException();
        } else {
            cache.put(token, TRUE);
        }
    }
}
