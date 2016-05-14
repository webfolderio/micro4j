package com.micro4j.sample.mvc.pjax;

import java.io.InputStream;

import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

@Path("/webjars")
public class AssetController {

    @GET
    @Path("/{asset: .*}")
    @Produces("application/javascript; charset=utf-8")
    public InputStream resource(@PathParam("asset") String asset) {
        InputStream is = getClass().getResourceAsStream("/META-INF/resources/webjars/" + asset);
        if (is == null) {
            throw new NotFoundException();
        }
        return is;
    }
}
