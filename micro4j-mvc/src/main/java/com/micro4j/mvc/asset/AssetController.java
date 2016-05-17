/* MIT License
 * 
 * Copyright (c) 2016 http://micro4j.com
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.micro4j.mvc.asset;

import static java.lang.String.valueOf;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.HttpHeaders.IF_MODIFIED_SINCE;
import static javax.ws.rs.core.HttpHeaders.LAST_MODIFIED;
import static javax.ws.rs.core.Response.ok;
import static javax.ws.rs.core.Response.status;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.NOT_MODIFIED;
import static javax.ws.rs.core.Response.Status.UNSUPPORTED_MEDIA_TYPE;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

import javax.ws.rs.GET;
import static com.micro4j.mvc.message.MvcMessages.*;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;

import com.micro4j.mvc.Configuration;

import static org.slf4j.LoggerFactory.getLogger;

public abstract class AssetController {

    private static final Logger LOG = getLogger(AssetController.class);

    @Context
    private HttpHeaders httpHeaders;

    private Configuration configuration;

    protected abstract String getPrefix();

    public AssetController(Configuration configuration) {
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
            String contentType = getContentType(asset, connection);
            if (contentType == null) {
                LOG.error(getString("AssetController.content.type.not.found"), new Object[] { asset }); //$NON-NLS-1$
                return status(UNSUPPORTED_MEDIA_TYPE).build();
            }
            return ok(connection.getInputStream())
                    .cacheControl(getCacheControl(asset))
                    .header(LAST_MODIFIED, lastModified)
                    .header(CONTENT_TYPE, contentType)
                    .build();
        }
    }

    protected String getContentType(String asset, URLConnection connection) {
        String ct = null;
        String charset = configuration.getCharset().name();
        if (asset.endsWith(".js")) {
            ct = "application/javascript; charset=" + charset; //$NON-NLS-1$
        } else if (asset.endsWith(".css")) { //$NON-NLS-1$
            ct = "text/css; charset=" + charset; //$NON-NLS-1$
        } else if (asset.endsWith(".woff")) { //$NON-NLS-1$
            ct = "application/font-woff"; //$NON-NLS-1$
        } else if (asset.endsWith(".woff2")) { //$NON-NLS-1$
            ct = "font/woff2"; //$NON-NLS-1$
        }
        return ct;
    }

    protected CacheControl getCacheControl(String asset) {
        CacheControl cache = new CacheControl();
        cache.setNoStore(false);
        cache.setMustRevalidate(true);
        return cache;
    }
}
