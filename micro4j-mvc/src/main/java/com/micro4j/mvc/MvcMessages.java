package com.micro4j.mvc;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.slf4j.Logger;

public class MvcMessages {

    private static final String BUNDLE_NAME = "com.micro4j.mvc.mvc-messages"; //$NON-NLS-1$

    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME);

    private static final Logger LOG = getLogger(MvcMessages.class);

    public static String getString(String key) {
        try {
            return RESOURCE_BUNDLE.getString(key);
        } catch (MissingResourceException e) {
            LOG.error(e.getMessage(), e);
            return '!' + key + '!';
        }
    }
}
