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
package com.micro4j.mvc.csrf;

import static javax.ws.rs.core.Response.Status.Family.SUCCESSFUL;

import java.io.IOException;
import java.util.Set;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;

import org.jboss.resteasy.core.interception.PreMatchContainerRequestContext;

import com.micro4j.mvc.MvcSession;

public class CsrfFilter implements ContainerResponseFilter {

    protected static final String SESSION_KEY_CSRF_TOKENS = "micro4j.csrf.tokens";

    protected static final String CSRF_TOKEN = "csrf-token";

    @Context
    protected MvcSession session;

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
            throws IOException {
        
        boolean successful = responseContext.getStatusInfo().getFamily().equals(SUCCESSFUL);

        if ( ! successful ) {
            return;
        }

        PreMatchContainerRequestContext ctx = (PreMatchContainerRequestContext) requestContext;
        MultivaluedMap<String,String> parameters = ctx.getHttpRequest().getFormParameters();
        
        if (parameters == null) {
            return;
        }

        String token = parameters.getFirst(CSRF_TOKEN);

        if (token == null || token.trim().isEmpty()) {
            return;
        }

        Set<String> tokens = session.get(SESSION_KEY_CSRF_TOKENS);

        if ( tokens == null || tokens.isEmpty() || ! tokens.contains(token) ) {
            throw new InvalidCsrfTokenException();
        } else {
            tokens.remove(token);
        }
    }
}
