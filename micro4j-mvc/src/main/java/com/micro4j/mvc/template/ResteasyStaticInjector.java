package com.micro4j.mvc.template;

import org.jboss.resteasy.spi.InjectorFactory;
import org.jboss.resteasy.spi.PropertyInjector;
import org.jboss.resteasy.spi.ResteasyProviderFactory;

public class ResteasyStaticInjector implements Injector {

    @Override
    public void inject(Processor processor) {
        ResteasyProviderFactory providerFactory = ResteasyProviderFactory.getInstance();
        InjectorFactory injectorFactory = providerFactory.getInjectorFactory();
        PropertyInjector injector = injectorFactory.createPropertyInjector(processor.getClass(), providerFactory);
        injector.inject(processor);
    }
}
