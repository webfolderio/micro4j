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
package com.micro4j.mvc.jaxrs;

import static javax.ws.rs.core.HttpHeaders.CACHE_CONTROL;
import static javax.ws.rs.core.MediaType.TEXT_HTML_TYPE;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;

import com.micro4j.mvc.View;
import com.micro4j.mvc.ViewModel;
import com.micro4j.mvc.mustache.MustacheContentLamabda;
import com.micro4j.mvc.template.TemplateEngine;

public class ViewWriter implements MessageBodyWriter<Object> {

    @Context
    private HttpHeaders headers;

    private final TemplateEngine engine;

    private static final String PJAX = "X-PJAX";

    public ViewWriter(TemplateEngine engine) {
        this.engine = engine;
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return isCompatibleMediaType(mediaType) && isView(type, annotations);
    }

    protected boolean isCompatibleMediaType(MediaType mediaType) {
        return TEXT_HTML_TYPE.isCompatible(mediaType);
    }

    protected boolean isView(Class<?> type, Annotation[] annotations) {
        return ViewModel.class.isAssignableFrom(type) || hasAnnotation(annotations);
    }

    protected boolean hasAnnotation(Annotation[] annotations) {
        if (annotations == null || annotations.length <= 0) {
            return false;
        }
        for (Annotation annotation : annotations) {
            if (View.class.isAssignableFrom(annotation.annotationType())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public long getSize(Object data, Class<?> type, Type genericType, Annotation[] annotations,
            MediaType mediaType) {
        return -1;
    }

    @Override
    public void writeTo(Object data, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream)
            throws IOException, WebApplicationException {
        String name = null;
        String container = null;
        Object context = null;
        if (data != null && ViewModel.class.isAssignableFrom(data.getClass())) {
            ViewModel<?> model = (ViewModel<?>) data;
            name = model.getName();
            container = model.getContainer();
            context = model.getEntity();
        } else {
            for (Annotation annotation : annotations) {
                if (View.class.isAssignableFrom(annotation.annotationType())) {
                    View view = (View) annotation;
                    name = view.value();
                    if ( ! view.container().trim().isEmpty() ) {
                        container = view.container();
                    }
                    context = data;
                    break;
                }
            }
        }
        writeTo(name, container, context, httpHeaders, entityStream);
    }

    protected void writeTo(String name, String container, Object context, MultivaluedMap<String, Object> httpHeaders,
            OutputStream entityStream) throws IOException {
        if (container == null) {
            container = engine.getConfiguration().getDefaultContainer();
        }
        if (!httpHeaders.containsKey(CACHE_CONTROL)) {
            httpHeaders.add(CACHE_CONTROL, "no-cache, no-store, must-revalidate");
        }
        Map<String, Object> parentContext = new LinkedHashMap<>();
        boolean isPjax = isPjaxRequest(httpHeaders);
        if (isPjax || container.trim().isEmpty()) {
            try (OutputStreamWriter writer = new OutputStreamWriter(entityStream, engine.getConfiguration().getCharset())) {
                engine.execute(name, context, parentContext, writer);
            }
        } else {
            StringWriter pageWriter = new StringWriter();
            engine.execute(name, context, parentContext, pageWriter);
            try (OutputStreamWriter containerWriter = new OutputStreamWriter(entityStream, engine.getConfiguration().getCharset())) {
                parentContext.put(engine.getConfiguration().getBodyName(), new MustacheContentLamabda(pageWriter.toString()));
                engine.execute(container, context, parentContext, containerWriter);
            }
        }
    }

    protected boolean isPjaxRequest(MultivaluedMap<String, Object> httpHeaders) {
        return headers.getHeaderString(PJAX) != null;
    }
}