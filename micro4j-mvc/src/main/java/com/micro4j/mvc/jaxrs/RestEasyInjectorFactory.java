package com.micro4j.mvc.jaxrs;

import java.util.Set;

import org.jboss.resteasy.core.InjectorFactoryImpl;
import org.jboss.resteasy.core.ValueInjector;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.jboss.resteasy.spi.metadata.Parameter;

class RestEasyInjectorFactory extends InjectorFactoryImpl {

    private final Set<String> excludes;

    public RestEasyInjectorFactory(Set<String> excludes) {
        this.excludes = excludes;
    }

    @Override
    public ValueInjector createParameterExtractor(Parameter parameter, ResteasyProviderFactory providerFactory) {
        String name = parameter.getParamName();
        if ( name != null &&
                    ! name.trim().isEmpty() &&
                    excludes.contains(name) ) {
            return super.createParameterExtractor(parameter, providerFactory);
        }
        switch (parameter.getParamType()) {
            case HEADER_PARAM:
                return new RestEasyHeaderParamInjector(parameter.getType(), parameter.getGenericType(),
                        parameter.getAccessibleObject(), parameter.getParamName(), parameter.getDefaultValue(),
                        parameter.getAnnotations(), providerFactory);
            case PATH_PARAM:
                return new RestEasyPathParamInjector(parameter.getType(), parameter.getGenericType(),
                        parameter.getAccessibleObject(), parameter.getParamName(), parameter.getDefaultValue(),
                        parameter.isEncoded(), parameter.getAnnotations(), providerFactory);
            case COOKIE_PARAM:
                return new RestEasyParamInjector(parameter.getType(), parameter.getGenericType(),
                        parameter.getAccessibleObject(), parameter.getParamName(), parameter.getDefaultValue(),
                        parameter.getAnnotations(), providerFactory);
            case FORM_PARAM:
                return new RestEasyFormParamInjector(parameter.getType(), parameter.getGenericType(),
                        parameter.getAccessibleObject(), parameter.getParamName(), parameter.getDefaultValue(),
                        parameter.isEncoded(), parameter.getAnnotations(), providerFactory);
            case QUERY_PARAM:
                return new RestEasyQueryParamInjector(parameter.getType(), parameter.getGenericType(),
                        parameter.getAccessibleObject(), parameter.getParamName(), parameter.getDefaultValue(),
                        parameter.isEncoded(), parameter.getAnnotations(), providerFactory);
            default:
                return super.createParameterExtractor(parameter, providerFactory);
        }
    }
}
