/* MIT License
 * 
 * Copyright (c) 2016 http://micro4j.com
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.micro4j.mvc.template;

import static com.micro4j.mvc.message.MvcMessages.getString;
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
            String content = toString(is);
            return content;
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
