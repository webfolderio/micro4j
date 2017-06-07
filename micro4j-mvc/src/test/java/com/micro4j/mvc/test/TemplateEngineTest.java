package com.micro4j.mvc.test;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static org.junit.Assert.assertEquals;
import static org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY;
import static org.slf4j.impl.SimpleLogger.LOG_FILE_KEY;

import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import com.micro4j.mvc.Configuration;
import com.micro4j.mvc.mustache.MustacheClasspathLoader;
import com.micro4j.mvc.mustache.MustacheLocalLoader;
import com.micro4j.mvc.mustache.MustacheTemplateEngine;
import com.micro4j.mvc.template.TemplateEngine;

public class TemplateEngineTest {

    private TemplateEngine engine;

    static {
        System.setProperty("org.jboss.logging.provider", "slf4j");
        System.setProperty(DEFAULT_LOG_LEVEL_KEY, "debug");
        System.setProperty(LOG_FILE_KEY, "System.out");
    }

    @Before
    public void init() {
        Configuration configuration = new Configuration.Builder().build();
        MustacheClasspathLoader loader = new MustacheClasspathLoader(configuration);
        engine = new MustacheTemplateEngine(loader, configuration);
    }

    @Test
    public void testHelloWorldTemplate() {
        StringWriter writer = new StringWriter();
        engine.execute("template/hello-world.html", emptyMap(), emptyMap(), writer);
        assertEquals("Hello, world!", writer.toString());
    }

    @Test
    public void testMessageTemplate() {
        StringWriter writer = new StringWriter();
        Map<String, Object> model = new HashMap<>();
        model.put("name", "foo");
        engine.execute("template/message.html", model, emptyMap(), writer);
        assertEquals("Hello, foo", writer.toString());
    }

    @Test
    public void testLocalTemplateLoader() throws Exception {
        Set<Path> templateDirectories = new HashSet<>(asList(Paths.get("src/test/resources").toAbsolutePath(), Paths.get("invalid-path")));

        Configuration configuration = new Configuration.Builder().build();

        MustacheLocalLoader loader = new MustacheLocalLoader(configuration, templateDirectories);
        TemplateEngine localTemplateManager = new MustacheTemplateEngine(loader, configuration);

        Reader reader = loader.getTemplate("template/hello-world.html");
        char[] buffer = new char[1024 * 4];
        reader.read(buffer);

        assertEquals("Hello, world!", new String(buffer).trim());
        
        Writer writer = new StringWriter();
        localTemplateManager.execute("template/hello-world.html", emptyMap(), emptyMap(), writer);
        assertEquals("Hello, world!", writer.toString());
    }
}
