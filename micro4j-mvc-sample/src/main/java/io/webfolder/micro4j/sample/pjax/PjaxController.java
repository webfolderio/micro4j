/**
 * The MIT License
 * Copyright © 2016 - 2018 WebFolder OÜ
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
package io.webfolder.micro4j.sample.pjax;

import static java.net.URI.create;
import static javax.ws.rs.core.Response.Status.MOVED_PERMANENTLY;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.RedirectionException;
import javax.ws.rs.core.MediaType;

import io.webfolder.micro4j.mvc.View;

@Path("/")
@Produces(MediaType.TEXT_HTML)
public class PjaxController {

    @GET
    @Path("/")
    public void index() {
        throw new RedirectionException("index", MOVED_PERMANENTLY, create("/page1"));
    }

    @GET
    @Path("/page1")
    @View(value = "view/pjax/page-1.html", container = "view/pjax/container.html")
    public Map<String, Object> page1() {
        Map<String, Object> model = new HashMap<>();
        model.put("title", "Page 1");
        return model;
    }

    @GET
    @Path("/page2")
    @View(value = "view/pjax/page-2.html", container = "view/pjax/container.html")
    public Map<String, Object> page2() {
        Map<String, Object> model = new HashMap<>();
        model.put("title", "Page 2");
        return model;
    }
}
