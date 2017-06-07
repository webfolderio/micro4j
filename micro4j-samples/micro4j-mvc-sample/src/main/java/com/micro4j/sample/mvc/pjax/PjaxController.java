package com.micro4j.sample.mvc.pjax;

import static java.net.URI.create;
import static javax.ws.rs.core.Response.Status.MOVED_PERMANENTLY;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.RedirectionException;
import javax.ws.rs.core.MediaType;

import com.micro4j.mvc.View;

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
