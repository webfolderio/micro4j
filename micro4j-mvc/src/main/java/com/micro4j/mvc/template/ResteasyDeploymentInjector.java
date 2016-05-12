package com.micro4j.mvc.template;

import org.jboss.resteasy.spi.InjectorFactory;
import org.jboss.resteasy.spi.PropertyInjector;
import org.jboss.resteasy.spi.ResteasyDeployment;
import org.jboss.resteasy.spi.ResteasyProviderFactory;

public class ResteasyDeploymentInjector implements Injector {

    private ResteasyDeployment deployment;

    public ResteasyDeploymentInjector(ResteasyDeployment deployment) {
        this.deployment = deployment;
    }

    @Override
    public void inject(Processor processor) {
        ResteasyProviderFactory providerFactory = deployment.getProviderFactory();
        InjectorFactory injectorFactory = providerFactory.getInjectorFactory();
        PropertyInjector injector = injectorFactory.createPropertyInjector(processor.getClass(), providerFactory);
        injector.inject(processor);
    }
}
