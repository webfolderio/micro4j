package com.micro4j.mvc.csrf;

import java.util.Map;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.MultivaluedMap;

import org.jboss.resteasy.core.interception.jaxrs.PostMatchContainerRequestContext;

public class RestEasyCsrfFilter extends AbstractCsrfFilter {

    public RestEasyCsrfFilter(Map<String, Boolean> cache) {
        super(cache);
    }

    @Override
    protected MultivaluedMap<String, String> getFormParameters(ContainerRequestContext requestContext) {
        PostMatchContainerRequestContext ctx = (PostMatchContainerRequestContext) requestContext;
        return ctx.getHttpRequest().getFormParameters();
    }
}
