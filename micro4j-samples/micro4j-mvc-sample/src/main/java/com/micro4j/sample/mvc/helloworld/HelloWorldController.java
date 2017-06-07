package com.micro4j.sample.mvc.helloworld;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import com.micro4j.mvc.View;

@Path("/")
public class HelloWorldController {

    @GET
    @Path("/")
    public void inde() {
        throw new WebApplicationException(Response.temporaryRedirect(URI.create("/hello")).build());
    }

    @GET
    @Path("/hello")
    @View("view/helloworld/page.html")
    public Map<String, Object> hello() {
        Map<String, Object> model = new HashMap<>();
        model.put("message", "hello, world!");
        return model;
    }
}
