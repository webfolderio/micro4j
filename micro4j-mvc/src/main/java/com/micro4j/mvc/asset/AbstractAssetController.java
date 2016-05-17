package com.micro4j.mvc.asset;

import static java.lang.String.valueOf;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.HttpHeaders.IF_MODIFIED_SINCE;
import static javax.ws.rs.core.HttpHeaders.LAST_MODIFIED;
import static javax.ws.rs.core.Response.ok;
import static javax.ws.rs.core.Response.status;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.NOT_MODIFIED;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import com.micro4j.mvc.Configuration;

public abstract class AbstractAssetController {

    @Context
    private HttpHeaders httpHeaders;

    protected abstract String getPrefix();

    private Configuration configuration;

    public AbstractAssetController(Configuration configuration) {
        this.configuration = configuration;
    }

    @GET
    @Path("/{asset: .*}")
    public Response resource(@PathParam("asset") String asset) throws IOException {
        String path = getPrefix() + asset;
        URL url = configuration.getClassLoader().getResource(path);
        if (url == null) {
            return status(NOT_FOUND).build();
        }
        URLConnection connection = url.openConnection();
        String lastModified = valueOf(connection.getLastModified());
        String ifModifiedSince = httpHeaders.getHeaderString(IF_MODIFIED_SINCE);
        if (lastModified.equals(ifModifiedSince)) {
            return status(NOT_MODIFIED).build();
        } else {
            String contentType = "application/javascript; charset=" + configuration.getCharset().name();
            return ok(connection.getInputStream())
                    .header(LAST_MODIFIED, lastModified)
                    .header(CONTENT_TYPE, contentType)
                    .build();
        }
    }
}
