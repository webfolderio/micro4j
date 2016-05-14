package com.micro4j.sample.mvc.helloworld;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

import com.micro4j.mvc.jaxrs.MvcFeature;

public class HelloWorldApplication extends Application {

    @Override
    public Set<Object> getSingletons() {
        Set<Object> singletons = new HashSet<>();
        singletons.add(new MvcFeature());
        singletons.add(new HelloWorldController());
        return singletons;
    }
}
