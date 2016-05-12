package com.micro4j.mvc.test;

import static org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY;
import static org.slf4j.impl.SimpleLogger.LOG_FILE_KEY;

import org.jboss.resteasy.plugins.server.sun.http.SunHttpJaxrsServer;
import org.jboss.resteasy.spi.ResteasyDeployment;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.slf4j.bridge.SLF4JBridgeHandler;

public class ResteasyViewTest extends ViewTest {

    private static ResteasyDeployment deployment;

    private static SunHttpJaxrsServer server;

    static {
        System.setProperty("org.jboss.logging.provider", "slf4j");
        System.setProperty(DEFAULT_LOG_LEVEL_KEY, "debug");
        System.setProperty(LOG_FILE_KEY, "System.out");
        SLF4JBridgeHandler.install();
    }

    @BeforeClass
    public static void before() {
        deployment = new ResteasyDeployment();
        deployment.setApplication(new MyApplication("resteasy"));

        server = new SunHttpJaxrsServer();
        server.setPort(4040);
        server.setRootResourcePath("/");
        server.setDeployment(deployment);

        server.start();

    }

    @AfterClass
    public static void after() {
        deployment.stop();
        server.stop();
    }
}
