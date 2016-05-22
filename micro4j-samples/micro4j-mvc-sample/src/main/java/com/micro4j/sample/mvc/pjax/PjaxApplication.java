package com.micro4j.sample.mvc.pjax;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

import com.micro4j.mvc.Configuration;
import com.micro4j.mvc.Configuration.Builder;
import com.micro4j.mvc.asset.AssetInterceptor;
import com.micro4j.mvc.jaxrs.MvcFeature;
import com.micro4j.mvc.jaxrs.WebJarController;

public class PjaxApplication extends Application {

    private Set<Object> singletons = new HashSet<>();

    public PjaxApplication() {
        Configuration configuration = new Builder()
                                        .interceptors(new AssetInterceptor())
                                        .build();
        singletons.add(new MvcFeature(configuration));
        boolean enableAssetGzip = true;
        singletons.add(new WebJarController(configuration, enableAssetGzip));
        singletons.add(new PjaxController());
    }

    @Override
    public Set<Object> getSingletons() {
        return singletons;
    }
}
