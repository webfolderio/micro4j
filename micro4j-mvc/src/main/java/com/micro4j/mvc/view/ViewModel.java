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
package com.micro4j.mvc.view;

import static java.nio.charset.StandardCharsets.UTF_8;
import static javax.ws.rs.core.MediaType.TEXT_HTML_TYPE;
import static javax.ws.rs.core.Response.status;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

public class ViewModel<T> {

    private static final MediaType HTML = TEXT_HTML_TYPE.withCharset(UTF_8.name());

    private String name;

    private String container;

    private T entity;

    public ViewModel(String name, T entity) {
        this(name, null, entity);
    }

    public ViewModel(String name, String container, T entity) {
        this.name = name;
        this.container = container;
        this.entity = entity;
    }

    public String getName() {
        return name;
    }

    public T getEntity() {
        return entity;
    }

    public Response toBadRequest() {
        return toResponse(BAD_REQUEST);
    }

    public Response toNotFound() {
        return toResponse(NOT_FOUND);
    }

    public Response toResponse(Status status) {
        return toResponse(status, HTML);
    }

    public Response toResponse(Status status, MediaType mediaType) {
        return status(status)
                    .type(mediaType)
                    .entity(this)
                    .build();
    }

    public String getContainer() {
        return container;
    }

    @Override
    public String toString() {
        return "ViewModel [name=" + name + ", container=" + container + ", entity=" + entity + "]";
    }
}
