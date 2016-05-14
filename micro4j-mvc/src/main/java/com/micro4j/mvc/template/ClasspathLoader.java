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

import java.io.InputStream;

import org.slf4j.Logger;

import static com.micro4j.mvc.message.MvcMessages.getString;
import com.micro4j.mvc.Configuration;

import static org.slf4j.LoggerFactory.getLogger;

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
            LOG.error(getString("MvcFeature.initialization.success"), new Object[] { name });
        }
        return is;
    }
}
