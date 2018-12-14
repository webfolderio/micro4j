/**
 * The MIT License
 * Copyright © 2016 - 2018 WebFolder OÜ
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
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.bridge.SLF4JBridgeHandler;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import io.webfolder.micro4j.mvc.Configuration;
import io.webfolder.micro4j.mvc.MvcMessages;
import io.webfolder.micro4j.mvc.View;
import io.webfolder.micro4j.mvc.ViewModel;
import io.webfolder.micro4j.mvc.jaxrs.MvcFeature;
import io.webfolder.micro4j.mvc.mustache.MustacheFormatter;
import io.webfolder.micro4j.mvc.mustache.MustacheI18nInterceptor;

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
        @Path("/assets")
        @View("template/assets.html")
        public Map<String, Object> assets() {
            Map<String, Object> model = new LinkedHashMap<>();
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
        @Path("/test-invalid-extension")
        public ViewModel<Map<String, Object>> testInvalidExtension() {
            ViewModel<Map<String, Object>> model = new ViewModel<Map<String,Object>>("template/message.htm", new LinkedHashMap<>());
            model.getEntity().put("name", "bar");
            return model;
        }

        @GET
        @Path("/test-without-extension")
        public ViewModel<Map<String, Object>> testWithoutExtension() {
            ViewModel<Map<String, Object>> model = new ViewModel<Map<String,Object>>("template/message", new LinkedHashMap<>());
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
            System.out.println(model.toString());
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

        public TestApplication(boolean readGz) {
            classes = new HashSet<>();

            Configuration defaultConfiguraton = Configuration.defaultConfiguration();

            Configuration configuration = new Configuration
                                                .Builder()
                                                .bodyName(defaultConfiguraton.getBodyName())
                                                .fileTypeExtensions("html")
                                                .container("")
                                                .classLoader(BaseTest.class.getClassLoader())
                                                .charset(Charset.forName("utf-8"))
                                                .nullValue("")
                                                .prefix("")
                                                .delims("{{ }}")
                                                .formatter(new MustacheFormatter())
                                                .enableTemplateCaching(true)
                                                .locale(Locale.ENGLISH)
                                                .interceptors(
                                                        new MustacheI18nInterceptor("template.myapp"))
                                                .build();

            System.out.println(configuration.toString());
            
            MvcFeature feature = new MvcFeature(configuration);

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

    @BeforeClass
    public static void setUp() {
        client = new OkHttpClient();
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

    @Test
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

    @Test
    public void testInclude() throws IOException {
        Request request = new Request.Builder().url("http://localhost:4040/test-include").build();
        Response response = client.newCall(request).execute();
        assertEquals("hello world", response.body().string());
    }

    @Test
    public void testInvalidExtension() throws IOException {
        Request request = new Request.Builder().url("http://localhost:4040/test-invalid-extension").build();
        Response response = client.newCall(request).execute();
        assertEquals("", response.body().string());
    }

    @Test
    public void testWithoutExtension() throws IOException {
        Request request = new Request.Builder().url("http://localhost:4040/test-without-extension").build();
        Response response = client.newCall(request).execute();
        assertEquals("", response.body().string());
    }

    @Test
    public void testMissingAsset() throws IOException {
        Request request = new Request.Builder().url("http://localhost:4040/webjars/mylib.css").build();
        Response response = client.newCall(request).execute();
        assertEquals(404, response.code());
    }

    @Test
    public void testOverrideI18n() throws IOException {
        Request request = new Request.Builder().url("http://localhost:4040/test-include?lang=es").build();
        Response response = client.newCall(request).execute();
        assertEquals("hello world", response.body().string());
    }

    @Test
    public void testMissingKey() {
        String value = MvcMessages.getString("invalid.key");
        assertEquals("!invalid.key!", value);
    }
}
