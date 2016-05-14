package com.micro4j.sample.mvc.pjax;

import java.io.InputStream;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

@Path("/webjars")
public class AssetController {

    @Context
    private UriInfo uriInfo;

    @GET
    @Path("/{asset: .*}")
    @Produces("application/javascript; charset=utf-8")
    public InputStream resource(@PathParam("asset") String asset) {
        return getClass().getResourceAsStream("/META-INF/resources/webjars/" + asset);
    }
}
