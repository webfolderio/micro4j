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

import static java.lang.Long.toHexString;

import java.security.SecureRandom;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.ws.rs.core.Context;

import com.micro4j.mvc.MvcSession;
import com.micro4j.mvc.template.TemplateIntereceptor;
import com.micro4j.mvc.template.TemplateWrapper;

public class MustacheCsrfInterceptor extends TemplateIntereceptor {

    protected static final String SESSION_KEY_CSRF_TOKENS = "micro4j.csrf.tokens";

    private final SecureRandom random = new SecureRandom();

    @Context
    private MvcSession session;

    @Override
    public TemplateWrapper beforeExecute(String name, TemplateWrapper templateWrapper, Object context,
            Map<String, Object> parentContext) {
        String token = toHexString(random.nextLong());
        Set<String> tokens = session.get(SESSION_KEY_CSRF_TOKENS);
        if (tokens == null) {
            tokens = new CopyOnWriteArraySet<String>();
            session.set(SESSION_KEY_CSRF_TOKENS, tokens);
        }
        tokens.add(token);
        parentContext.put("csrf-token", token);
        return templateWrapper;
    }
}
