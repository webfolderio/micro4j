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

import static org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY;
import static org.slf4j.impl.SimpleLogger.LOG_FILE_KEY;

import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;

import org.jboss.resteasy.plugins.server.sun.http.SunHttpJaxrsServer;
import org.jboss.resteasy.spi.ResteasyDeployment;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import com.micro4j.mvc.mustache.MustacheI18nProcessor;
import com.micro4j.mvc.mustache.MustacheTemplateEngine;
import com.micro4j.mvc.template.Configuration;
import com.micro4j.mvc.view.View;
import com.micro4j.mvc.view.ViewWriter;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.logging.HttpLoggingInterceptor;
import com.squareup.okhttp.logging.HttpLoggingInterceptor.Level;
import com.squareup.okhttp.logging.HttpLoggingInterceptor.Logger;

public class ViewTest {

    static {
        System.setProperty("org.jboss.logging.provider", "slf4j");
        System.setProperty(DEFAULT_LOG_LEVEL_KEY, "debug");
        System.setProperty(LOG_FILE_KEY, "System.out");
    }

    @Path("/")
    @Produces(MediaType.TEXT_HTML)
    public static class SampleController {

        @GET
        @Path("/")
        @View("template/message.html")
        public Map<String, Object> message() {
            Map<String, Object> model = new LinkedHashMap<>();
            model.put("name", "foo");
            return model;
        }

        @GET
        @Path("/page")
        @View(value = "template/page.html", container = "template/container.html")
        public Map<String, Object> page() {
            Map<String, Object> model = new LinkedHashMap<>();
            return model;
        }

        @GET
        @Path("/my-form")
        @View("template/myform.html")
        public Map<String, Object> formPage() {
            Map<String, Object> model = new LinkedHashMap<>();
            model.put("firstName", "foo");
            model.put("surname", "bar");
            return model;
        }
    }

    public static class MyApplication extends Application {

        private Set<Class<?>> classes;

        private Set<Object> singletons;

        public MyApplication() {
            classes = new HashSet<>();
            
            Configuration configuration = new Configuration
                                                .Builder()
                                                .processors(new MustacheI18nProcessor("template.myapp"))
                                                .build();

            singletons = new HashSet<>();
            singletons.add(new ViewWriter(new MustacheTemplateEngine(configuration)));
            singletons.add(new SampleController());
        }

        @Override
        public Set<Object> getSingletons() {
            return singletons;
        }

        @Override
        public Set<Class<?>> getClasses() {
            return classes;
        }
    }

    private static ResteasyDeployment deployment;

    private static SunHttpJaxrsServer server;

    private static OkHttpClient client;

    private static class HttpLogger implements Logger {

        private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(HttpLogger.class);

        @Override
        public void log(String message) {
            LOG.debug(message);;
        }
    }

    @BeforeClass
    public static void before() {
        deployment = new ResteasyDeployment();
        deployment.setApplication(new MyApplication());

        server = new SunHttpJaxrsServer();
        server.setPort(4040);
        server.setRootResourcePath("/");
        server.setDeployment(deployment);

        server.start();

        client = new OkHttpClient();

        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor(new HttpLogger());
        loggingInterceptor.setLevel(Level.BODY);
        client.interceptors().add(loggingInterceptor);
    }

    public static void after() {
        deployment.stop();
        server.stop();
    }

    @Test
    public void testView() throws IOException {
        client.setConnectTimeout(60, TimeUnit.SECONDS);
        client.setReadTimeout(60, TimeUnit.SECONDS);
        client.setWriteTimeout(60, TimeUnit.SECONDS);

        Request request = new Request.Builder().url("http://localhost:4040").build();
        Response response = client.newCall(request).execute();

        Assert.assertEquals("Hello, foo", response.body().string());

        Request pageRequest = new Request.Builder().url("http://localhost:4040/page").build();
        String pageContent = client.newCall(pageRequest).execute().body().string();
        Assert.assertEquals("<html><body>hi!</body></html>", pageContent);

        Request pjaxRequest = new Request.Builder().url("http://localhost:4040/page").header("X-PJAX", "true").build();
        Response pjaxResponse = client.newCall(pjaxRequest).execute();
        Assert.assertEquals("no-cache, no-store, must-revalidate", pjaxResponse.header("Cache-Control"));
        Assert.assertEquals("0", pjaxResponse.header("Expires"));
        String pjaxContent = pjaxResponse.body().string();

        Assert.assertEquals("hi!", pjaxContent);
    }
}
