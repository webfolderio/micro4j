package com.micro4j.mvc.test;

import static org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY;
import static org.slf4j.impl.SimpleLogger.LOG_FILE_KEY;

import java.io.IOException;
import java.net.URI;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.simple.SimpleContainerFactory;
import org.glassfish.jersey.simple.SimpleServer;
import org.junit.AfterClass;
import org.junit.BeforeClass;

public class JerseyViewTest extends ViewTest {

    static {
        System.setProperty(DEFAULT_LOG_LEVEL_KEY, "debug");
        System.setProperty(LOG_FILE_KEY, "System.out");
    }

    private static SimpleServer server;
 
    @BeforeClass
    public static void before() {
        ResourceConfig config = ResourceConfig.forApplication(new MyApplication("jersey"));
        server = SimpleContainerFactory.create(URI.create("http://localhost:4040"), config);
    }

    @AfterClass
    public static void after() throws IOException {
        server.close();
    }
}
