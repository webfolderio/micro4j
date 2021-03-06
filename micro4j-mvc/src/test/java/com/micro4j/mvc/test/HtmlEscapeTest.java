/**
 * The MIT License
 * Copyright © 2016 - 2020 WebFolder OÜ
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.micro4j.mvc.test;

import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Application;

import org.jboss.resteasy.plugins.server.sun.http.SunHttpJaxrsServer;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.squareup.okhttp.FormEncodingBuilder;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import io.webfolder.micro4j.mvc.Configuration.Builder;
import io.webfolder.micro4j.mvc.jaxrs.MvcFeature;

public class HtmlEscapeTest {

    private static class TestApplication extends Application {

        @Override
        public Set<Object> getSingletons() {
            Set<Object> singletons = new HashSet<>();
            singletons.add(new MvcFeature(new Builder().escapeHtml(true).build()));
            singletons.add(new TestController());
            return singletons;
        }
    }

    private static SunHttpJaxrsServer server;

    private static OkHttpClient client;

    @BeforeClass
    public static void beforeTest() {
        server = new SunHttpJaxrsServer();
        server.getDeployment().setApplication(new TestApplication());
        server.setPort(4040);
        server.setRootResourcePath("/");
        server.start();

        client = new OkHttpClient();
        client.setConnectTimeout(60, SECONDS);
        client.setReadTimeout(60, SECONDS);
        client.setWriteTimeout(60, SECONDS);
    }

    
    @AfterClass
    public static void afterTest() {
        if (server != null) {
            server.stop();
        }
    }

    @Path("/")
    public static class TestController {

        @GET
        @Path("/page")
        public String page(@QueryParam("param") String param) {
            return param;
        }

        @POST
        @Path("/form")
        public String form(@FormParam("param") List<String> params) {
            return params.toString();
        }
    }

    @Test
    public void test() throws Exception {
        Request request = new Request.Builder().url("http://localhost:4040/page?param=<div click='javascript: alert('hello');'>").get().build();
        Response response = client.newCall(request).execute();
        Assert.assertEquals("&lt;div click&#x3D;&#39;javascript: alert(&#39;hello&#39;);&#39;&gt;", response.body().string());

        RequestBody body = new FormEncodingBuilder().add("param", "<p1>").add("param", "<p2>").build();
        request = new Request.Builder().url("http://localhost:4040/form").post(body).build();
        response = client.newCall(request).execute();

        Assert.assertEquals("[&lt;p1&gt;, &lt;p2&gt;]", response.body().string());
    }
}
