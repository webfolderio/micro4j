/**
 * The MIT License
 * Copyright © 2016 - 2017 WebFolder OÜ
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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;

import org.jboss.resteasy.plugins.server.sun.http.SunHttpJaxrsServer;
import org.jboss.resteasy.spi.ResteasyDeployment;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.squareup.okhttp.FormEncodingBuilder;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import io.webfolder.micro4j.mvc.View;
import io.webfolder.micro4j.mvc.Configuration.Builder;
import io.webfolder.micro4j.mvc.csrf.CsrfFeature;
import io.webfolder.micro4j.mvc.csrf.EnableCsrfFilter;
import io.webfolder.micro4j.mvc.csrf.MustacheCsrfInterceptor;
import io.webfolder.micro4j.mvc.jaxrs.MvcFeature;

public class CsrfTest {

    private static class TestApplication extends Application {

        @Override
        public Set<Object> getSingletons() {
            Map<String, Boolean> cache = new ConcurrentHashMap<>();
            Set<Object> singletons = new HashSet<>();
            singletons.add(new MvcFeature(
                        new Builder()
                            .interceptors(new MustacheCsrfInterceptor(cache))                            
                        .build()));
            singletons.add(new CsrfFeature(cache));
            singletons.add(new TestController());
            return singletons;
        }
    }

    private static ResteasyDeployment deployment;

    private static SunHttpJaxrsServer server;

    private static OkHttpClient client;

    @BeforeClass
    public static void beforeTest() {
        deployment = new ResteasyDeployment();
        deployment.setApplication(new TestApplication());
        server = new SunHttpJaxrsServer();
        server.setPort(4040);
        server.setRootResourcePath("/");
        server.setDeployment(deployment);
        server.start();

        client = new OkHttpClient();
        client.setConnectTimeout(60, SECONDS);
        client.setReadTimeout(60, SECONDS);
        client.setWriteTimeout(60, SECONDS);
    }

    @AfterClass
    public static void afterTest() {
        if (deployment != null) {
            deployment.stop();
        }
        if (server != null) {
            server.stop();
        }
    }

    @Path("/")
    @EnableCsrfFilter
    public static class TestController {

        @GET
        @Path("/page")
        @Produces(MediaType.TEXT_HTML)
        @View("template/csrf-page.html")
        public Map<Object, Object> page() {
            return new HashMap<>();
        }

        @POST
        @Path("/form")
        public void form(@FormParam("param") List<String> params) {
        }
    }

    @Test
    public void test() throws Exception {
        Request request = new Request.Builder().url("http://localhost:4040/page").get().build();
        Response response = client.newCall(request).execute();

        String csrfToken = response.body().string();

        Assert.assertNotNull(csrfToken);

        RequestBody body = new FormEncodingBuilder()
                                .add("param", "myvalue")
                                .add("csrf-token", csrfToken)
                            .build();

        request = new Request.Builder().url("http://localhost:4040/form").post(body).build();
        response = client.newCall(request).execute();

        Assert.assertTrue(response.isSuccessful());

        body = new FormEncodingBuilder()
                .add("param", "myvalue-2")
                .add("csrf-token", csrfToken)
            .build();

        request = new Request.Builder().url("http://localhost:4040/form").post(body).build();
        response = client.newCall(request).execute();

        Assert.assertFalse(response.isSuccessful());
    }
}
