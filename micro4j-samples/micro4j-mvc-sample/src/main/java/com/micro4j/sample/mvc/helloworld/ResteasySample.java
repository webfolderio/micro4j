package com.micro4j.sample.mvc.helloworld;

import org.jboss.resteasy.plugins.server.sun.http.SunHttpJaxrsServer;
import org.jboss.resteasy.spi.ResteasyDeployment;

public class ResteasySample {

    public static void main(String[] args) {
        ResteasyDeployment deployment = new ResteasyDeployment();
        deployment.setApplication(new HelloWorldApplication());
        SunHttpJaxrsServer server = new SunHttpJaxrsServer();
        server.setPort(8080);
        server.setRootResourcePath("/");
        server.setDeployment(deployment);
        server.start();
    }
}
