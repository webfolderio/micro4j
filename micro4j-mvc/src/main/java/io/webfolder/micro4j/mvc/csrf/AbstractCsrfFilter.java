/**
 * The MIT License
 * Copyright © 2016 - 2019 WebFolder OÜ
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package io.webfolder.micro4j.mvc.csrf;

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
