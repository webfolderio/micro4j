package com.micro4j.sample.mvc.pjax;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

import com.micro4j.mvc.Configuration;
import com.micro4j.mvc.Configuration.Builder;
import com.micro4j.mvc.jaxrs.MvcFeature;

public class PjaxApplication extends Application {

    @Override
    public Set<Object> getSingletons() {
        Set<Object> singletons = new HashSet<>();
        Configuration configuration = new Builder().processors(new AssetProcessor()).build();
        singletons.add(new MvcFeature(configuration));
        singletons.add(new AssetController());
        singletons.add(new PjaxController());
        return singletons;
    }
}
