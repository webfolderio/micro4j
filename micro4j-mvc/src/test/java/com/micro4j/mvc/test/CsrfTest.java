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
package com.micro4j.mvc.test;

import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

import com.micro4j.mvc.Configuration.Builder;
import com.micro4j.mvc.MvcSession;
import com.micro4j.mvc.View;
import com.micro4j.mvc.csrf.MustacheCsrfInterceptor;
import com.micro4j.mvc.jaxrs.MvcFeature;
import com.squareup.okhttp.FormEncodingBuilder;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

public class CsrfTest {

    private static class TestApplication extends Application {

        @Override
        public Set<Object> getSingletons() {
            Set<Object> singletons = new HashSet<>();
            singletons.add(new MvcFeature(
                        new Builder()
                            .interceptors(new MustacheCsrfInterceptor())                            
                            .enableCsrfProtection(true)
                        .build()));
            singletons.add(new DummySession());
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
        deployment.getDefaultContextObjects().put(MvcSession.class, new DummySession());
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
        Request request1 = new Request.Builder().url("http://localhost:4040/page").get().build();
        Response response1 = client.newCall(request1).execute();

        String csrfToken = response1.body().string();

        Assert.assertNotNull(csrfToken);

        RequestBody body = new FormEncodingBuilder()
                                .add("param", "myvalue")
                                .add("csrf-token", csrfToken)
                            .build();

        Request request2 = new Request.Builder().url("http://localhost:4040/form").post(body).build();
        Response response2 = client.newCall(request2).execute();

        Assert.assertTrue(response2.isSuccessful());

        body = new FormEncodingBuilder()
                .add("param", "myvalue-2")
                .add("csrf-token", csrfToken)
            .build();

        request2 = new Request.Builder().url("http://localhost:4040/form").post(body).build();
        response2 = client.newCall(request2).execute();

        Assert.assertFalse(response2.isSuccessful());
    }
}
