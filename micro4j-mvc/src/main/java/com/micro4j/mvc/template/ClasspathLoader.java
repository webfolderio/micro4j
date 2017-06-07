package com.micro4j.mvc.template;

import static com.micro4j.mvc.MvcMessages.getString;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.InputStream;

import org.slf4j.Logger;

import com.micro4j.mvc.Configuration;

public abstract class ClasspathLoader extends AbstractContentLoader {

    private static final Logger LOG = getLogger(ClasspathLoader.class);

    public ClasspathLoader(Configuration configuration) {
        super(configuration);
    }

    @Override
    public InputStream getContent(String name) {
        ClassLoader cl = configuration.getClassLoader();
        name = configuration.getPrefix() + name;
        InputStream is = cl.getResourceAsStream(name);
        if (is == null) {
            LOG.error(getString("MvcFeature.initialization.success"), new Object[] { name }); // $NON-NLS-1$
        }
        return is;
    }
}
