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
package io.webfolder.micro4j.mvc.mustache;

import static io.webfolder.micro4j.mvc.MvcMessages.getString;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.slf4j.Logger;

import com.samskivert.mustache.Mustache.Lambda;
import com.samskivert.mustache.Template.Fragment;

public class MustacheI18nLambda implements Lambda {

    private final Map<String, String> templates = new HashMap<>();

    private ResourceBundle bundle;

    private final boolean enableCache;

    private static final Logger LOG = getLogger(MustacheI18nLambda.class);

    public MustacheI18nLambda(ResourceBundle bundle, boolean enableCache) {
        this.enableCache = enableCache;
        if (enableCache) {
            for (String key : bundle.keySet()) {
                String value = bundle.getString(key);
                templates.put(key, value);
            }
        } else {
            this.bundle = bundle;
        }
    }

    @Override
    public void execute(Fragment frag, Writer out) throws IOException {
        String key = frag.execute();
        if (key == null) {
            out.write("");
        }
        String value = null;
        if (enableCache) {
            value = templates.get(key);
        } else {
            try {
                value = bundle.getString(key);
            } catch (MissingResourceException e) {
            }
        }
        if (value != null) {
            out.write(value);
            return;
        }
        LOG.error(getString("MustacheI18nLambda.key.not.found"), new Object[] { key });
        out.write(key);
    }
}
