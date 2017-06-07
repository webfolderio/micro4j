package com.micro4j.mvc.template;

import static com.micro4j.mvc.MvcMessages.getString;
import static java.lang.String.join;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;

import org.slf4j.Logger;

import com.micro4j.mvc.Configuration;

public abstract class AbstractContentLoader implements ContentLoader {

    private static final Logger LOG = getLogger(AbstractContentLoader.class);

    protected Configuration configuration;

    public abstract InputStream getContent(String name);

    public AbstractContentLoader(Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    public String get(String name) {
        int begin = name.lastIndexOf(".");
        String extension = null;
        if (begin > 0) {
            extension = name.substring(begin + 1);
        }
        if (extension == null || extension.trim().isEmpty() || !configuration.getExtensions().contains(extension)) {
            LOG.error(getString("AbstractContentLoader.missing.file.extension"), //$NON-NLS-1$
                    new Object[] { join(",", configuration.getExtensions()) });
            return getDefaultContent();
        }
        try (InputStream is = getContent(name)) {
            LOG.info(getString("AbstractContentLoader.loading"), //$NON-NLS-1$
                    new Object[] { name });
            if (is == null) {
                LOG.error(getString("AbstractContentLoader.template.not.found"), //$NON-NLS-1$
                        new Object[] { name });
                return getDefaultContent();
            }
            return toString(is);
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
            return getDefaultContent();
        }
    }

    protected String toString(InputStream is) {
        String content = getDefaultContent();
        try (Scanner scanner = new Scanner(is, configuration.getCharset().name())) {
            scanner.useDelimiter("\\A");
            if (scanner.hasNext()) {
                content = scanner.next();
            }
        }
        return content;
    }

    protected String getDefaultContent() {
        return "";
    }
}
