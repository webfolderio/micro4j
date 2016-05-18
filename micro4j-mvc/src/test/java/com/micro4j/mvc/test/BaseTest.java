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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;
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

import com.micro4j.mvc.Configuration;
import com.micro4j.mvc.View;
import com.micro4j.mvc.ViewModel;
import com.micro4j.mvc.asset.AssetProcessor;
import com.micro4j.mvc.asset.AssetScanner;
import com.micro4j.mvc.asset.WebJarScanner;
import com.micro4j.mvc.jaxrs.MvcFeature;
import com.micro4j.mvc.jaxrs.WebJarController;
import com.micro4j.mvc.message.MvcMessages;
import com.micro4j.mvc.mustache.MustacheI18nProcessor;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

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

        public TestApplication(boolean readGz) {
            classes = new HashSet<>();

            AssetScanner scanner = new WebJarScanner() {

                @Override
                protected int getPriority(String path) {
                    if ("webjars/lib.js".equals(path)) {
                        return MAX_PRIORITY + 1;
                    }
                    return super.getPriority(path);
                }

                @Override
                protected boolean isAsset(String path) {
                    if (path.contains("bootstrap") && !path.endsWith("bootstrap.css")) {
                        return false;
                    }
                    boolean asset = (isJs(path) && !isRequireJs(path)) || isCss(path);
                    return asset;
                }
            };

            Configuration configuration = new Configuration
                                                .Builder()
                                                .processors(new MustacheI18nProcessor("template.myapp"), new AssetProcessor(scanner))
                                                .build();

            MvcFeature feature = new MvcFeature(configuration);

            singletons = new HashSet<>();
            singletons.add(new WebJarController(feature.getConfiguration(), readGz));
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
    public void testRequestJs() throws IOException {
        Request request = new Request.Builder().url("http://localhost:4040/webjars/lib.js").build();
        Response response = client.newCall(request).execute();
        assertEquals("console.info('foo');", response.body().string());
    }

    @Test
    public void testRequestCss() throws IOException {
        Request request = new Request.Builder().url("http://localhost:4040/webjars/bootstrap/3.3.6/css/bootstrap.css").build();
        Response response = client.newCall(request).execute();
        assertTrue(response.body().string().startsWith("/*!"));
    }

    @Test
    public void testMissingAsset() throws IOException {
        Request request = new Request.Builder().url("http://localhost:4040/webjars/mylib.css").build();
        Response response = client.newCall(request).execute();
        assertEquals(404, response.code());
    }

    @Test
    public void testLastModified() throws IOException {
        Request request = new Request.Builder().url("http://localhost:4040/webjars/bootstrap/3.3.6/css/bootstrap.css").build();
        Response response = client.newCall(request).execute();
        assertEquals(200, response.code());
        String lastModified = response.header(HttpHeaders.LAST_MODIFIED);
        request = new Request.Builder().url("http://localhost:4040/webjars/bootstrap/3.3.6/css/bootstrap.css").header(HttpHeaders.IF_MODIFIED_SINCE, lastModified).build();
        response = client.newCall(request).execute();
        assertEquals(304, response.code());
    }

    @Test
    public void testAssetProcessor() throws IOException {
        Request request = new Request.Builder().url("http://localhost:4040/assets").build();
        Response response = client.newCall(request).execute();
        Scanner scanner = new Scanner(response.body().string());
        String line1 = scanner.nextLine();
        String line2 = scanner.nextLine();
        String line3 = scanner.nextLine();
        String line4 = scanner.nextLine();
        assertEquals("<link type=\"text/css\" rel=\"stylesheet\" href=\"webjars/bootstrap/3.3.6/css/bootstrap.css\"></link>", line1);
        assertEquals("<script type=\"text/javascript\" src=\"webjars/lib.js\"></script>", line2);
        assertEquals("<script type=\"text/javascript\" src=\"webjars/jquery/1.11.1/jquery.js\"></script>", line3);
        assertEquals("<script type=\"text/javascript\" src=\"webjars/jquery-pjax/1.9.6/jquery.pjax.js\"></script>", line4);
        scanner.close();
    }

    @Test
    public void testMissingKey() {
        String value = MvcMessages.getString("invalid.key");
        assertEquals("!invalid.key!", value);
    }
}
