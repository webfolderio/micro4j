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

import static com.squareup.okhttp.logging.HttpLoggingInterceptor.Level.BODY;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.bridge.SLF4JBridgeHandler;

import com.micro4j.mvc.Configuration;
import com.micro4j.mvc.View;
import com.micro4j.mvc.ViewModel;
import com.micro4j.mvc.jaxrs.MvcFeature;
import com.micro4j.mvc.mustache.MustacheI18nProcessor;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.logging.HttpLoggingInterceptor;
import com.squareup.okhttp.logging.HttpLoggingInterceptor.Logger;

public abstract class BaseTest {

    private static OkHttpClient client;

    static {
        SLF4JBridgeHandler.install();
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

        @GET
        @Path("/test-return-view-model")
        public ViewModel<Map<String, Object>> testReturnViewModel() {
            ViewModel<Map<String, Object>> model = new ViewModel<Map<String,Object>>("template/message.html", new LinkedHashMap<>());
            model.getEntity().put("name", "bar");
            return model;
        }

        @GET
        @Path("/test-include")
        public ViewModel<Map<String, Object>> include() {
            ViewModel<Map<String, Object>> model = new ViewModel<Map<String,Object>>("template/include.html", new LinkedHashMap<>());
            model.getEntity().put("name", "bar");
            return model;
        }

        @GET
        @Path("/test-caching")
        public javax.ws.rs.core.Response overrideCaching() {
            ViewModel<Map<String, Object>> model = new ViewModel<Map<String,Object>>("template/include.html", new LinkedHashMap<>());
            model.getEntity().put("name", "bar");
            CacheControl caching = new CacheControl();
            caching.setMaxAge(200);
            caching.setMustRevalidate(true);
            caching.setPrivate(false);
            caching.setProxyRevalidate(false);
            caching.setNoStore(false);
            caching.setNoCache(false);
            return javax.ws.rs.core.Response.ok(model).cacheControl(caching).build();
        }
    }

    public static class TestApplication extends Application {

        private Set<Class<?>> classes;

        private Set<Object> singletons;

        public TestApplication() {
            classes = new HashSet<>();

            Configuration configuration = new Configuration
                                                .Builder()
                                                .processors(new MustacheI18nProcessor("template.myapp"))
                                                .build();

            Feature feature = new MvcFeature(configuration);

            singletons = new HashSet<>();
            singletons.add(new SampleController());
            singletons.add(feature);
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

    private static class HttpLogger implements Logger {

        private static final org.slf4j.Logger LOG = getLogger(HttpLogger.class);

        @Override
        public void log(String message) {
            LOG.debug(message);
        }
    }

    @BeforeClass
    public static void setUp() {
        client = new OkHttpClient();
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor(new HttpLogger());
        loggingInterceptor.setLevel(BODY);
        client.interceptors().add(loggingInterceptor);
        client.setConnectTimeout(60, SECONDS);
        client.setReadTimeout(60, SECONDS);
        client.setWriteTimeout(60, SECONDS);
    }

    @Test
    public void testHello() throws IOException {
        Request request = new Request.Builder().url("http://localhost:4040").build();
        Response response = client.newCall(request).execute();
        assertEquals("Hello, foo", response.body().string());

        Request pageRequest = new Request.Builder().url("http://localhost:4040/page").build();
        String pageContent = client.newCall(pageRequest).execute().body().string();
        assertEquals("<html><body>hi!</body></html>", pageContent);
    }

    public void testViewModel() throws IOException {
        Request request = new Request.Builder().url("http://localhost:4040/test-return-view-model").build();
        Response response = client.newCall(request).execute();
        assertEquals("Hello, bar", response.body().string());
    }

    @Test
    public void testPjax() throws IOException {
        Request pjaxRequest = new Request.Builder().url("http://localhost:4040/page").header("X-PJAX", "true").build();
        Response pjaxResponse = client.newCall(pjaxRequest).execute();
        assertEquals("no-cache, no-store, must-revalidate", pjaxResponse.header("Cache-Control"));
        String pjaxContent = pjaxResponse.body().string();
        assertEquals("hi!", pjaxContent);        
    }

    @Test
    public void testCaching() throws IOException {
        Request request = new Request.Builder().url("http://localhost:4040/test-caching").build();
        Response response = client.newCall(request).execute();
        assertEquals("must-revalidate, no-transform, max-age=200", response.header(HttpHeaders.CACHE_CONTROL));
    }

    public void testInclude() throws IOException {
        Request request = new Request.Builder().url("http://localhost:4040/test-include").build();
        Response response = client.newCall(request).execute();
        assertEquals("hello world", response.body().string());
    }
}
