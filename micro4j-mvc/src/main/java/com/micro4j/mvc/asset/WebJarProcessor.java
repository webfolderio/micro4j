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
package com.micro4j.mvc.asset;

import static com.micro4j.mvc.message.MvcMessages.getString;
import static java.util.Collections.emptyList;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;

import com.micro4j.mvc.template.Processor;
import com.micro4j.mvc.template.TemplateWrapper;

public class WebJarProcessor extends Processor {

    private static final Logger LOG = getLogger(WebJarProcessor.class);

    private List<String> assets = emptyList();

    @Override
    public void init() {
        assets = new WebJarScanner(configuration).scan();
        for (String asset : assets) {
            LOG.info(getString("WebJarProcessor.found.asset"), new Object[] { asset });
        }
    }

    @Override
    public TemplateWrapper beforeExecute(String name, TemplateWrapper templateWrapper, Object context,
            Map<String, Object> parentContext) {
        parentContext.put("assets", assets);
        return templateWrapper;
    }
}
