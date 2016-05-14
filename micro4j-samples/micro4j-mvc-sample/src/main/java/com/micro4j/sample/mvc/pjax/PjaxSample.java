package com.micro4j.sample.mvc.pjax;

import static java.net.URI.create;
import static org.glassfish.jersey.server.ResourceConfig.forApplication;
import static org.glassfish.jersey.simple.SimpleContainerFactory.create;

import org.glassfish.jersey.server.ResourceConfig;

public class PjaxSample {

    public static void main(String[] args) {
        ResourceConfig resourceConfig = forApplication(new PjaxApplication());
        create(create("http://localhost:8080"), resourceConfig);
    }
}
