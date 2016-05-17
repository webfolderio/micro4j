package com.micro4j.sample.mvc.pjax;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

import com.micro4j.mvc.Configuration;
import com.micro4j.mvc.Configuration.Builder;
import com.micro4j.mvc.asset.WebJarProcessor;
import com.micro4j.mvc.asset.WebJarController;
import com.micro4j.mvc.jaxrs.MvcFeature;

public class PjaxApplication extends Application {

    private Set<Object> singletons = new HashSet<>();

    public PjaxApplication() {
        Configuration configuration = new Builder()
                                        .processors(new WebJarProcessor())
                                        .build();

        singletons.add(new MvcFeature(configuration));
        singletons.add(new WebJarController(configuration));
        singletons.add(new PjaxController());
    }

    @Override
    public Set<Object> getSingletons() {
        return singletons;
    }
}
