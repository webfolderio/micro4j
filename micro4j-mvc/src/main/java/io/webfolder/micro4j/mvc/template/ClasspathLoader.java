/**
 * The MIT License
 * Copyright © 2016 - 2020 WebFolder OÜ
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package io.webfolder.micro4j.mvc.template;

import static io.webfolder.micro4j.mvc.MvcMessages.getString;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.InputStream;

import org.slf4j.Logger;

import io.webfolder.micro4j.mvc.Configuration;

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
