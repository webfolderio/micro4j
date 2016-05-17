package com.micro4j.mvc.asset;

import javax.ws.rs.Path;

import com.micro4j.mvc.Configuration;

@Path("/webjars")
public class WebJarController extends AssetController {

    public WebJarController(Configuration configuration) {
        super(configuration);
    }

    @Override
    protected String getPrefix() {
        return "META-INF/resources/webjars/";
    }
}
