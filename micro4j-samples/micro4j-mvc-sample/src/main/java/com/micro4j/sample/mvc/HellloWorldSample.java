package com.micro4j.sample.mvc;

import static java.net.URI.create;
import static org.glassfish.jersey.server.ResourceConfig.forApplication;
import static org.glassfish.jersey.simple.SimpleContainerFactory.create;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Application;

import org.glassfish.jersey.server.ResourceConfig;

import com.micro4j.mvc.jaxrs.MvcFeature;
import com.micro4j.mvc.view.View;

public class HellloWorldSample {

    public static class MvcApplication extends Application {

        @Override
        public Set<Object> getSingletons() {
            Set<Object> singletons = new HashSet<>();
            singletons.add(new MvcFeature());
            singletons.add(new HelloWorld());
            return singletons;
        }
    }

    @Path("/")
    public static class HelloWorld {

        @GET
        @Path("/hello")
        @View("view/helloworld/page.html")
        public Map<String, Object> hello() {
            Map<String, Object> model = new HashMap<>();
            model.put("message", "hello, world!");
            return model;
        }
    }

    public static void main(String[] args) {
        ResourceConfig resourceConfig = forApplication(new MvcApplication());
        create(create("http://localhost:8080"), resourceConfig);
    }
}
