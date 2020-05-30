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

import static java.lang.Boolean.FALSE;
import static java.lang.Long.toHexString;
import static org.jboss.resteasy.core.ResteasyContext.getContextDataLevelCount;

import java.security.SecureRandom;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

import io.webfolder.micro4j.mvc.template.TemplateIntereceptor;
import io.webfolder.micro4j.mvc.template.TemplateWrapper;

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
