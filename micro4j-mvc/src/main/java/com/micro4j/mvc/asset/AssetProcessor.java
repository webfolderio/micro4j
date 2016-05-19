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
import static org.slf4j.LoggerFactory.getLogger;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;

import com.micro4j.mvc.mustache.MustacheContentLamabda;
import com.micro4j.mvc.template.Processor;
import static java.lang.String.format;
import com.micro4j.mvc.template.TemplateWrapper;

public class AssetProcessor extends Processor {

    private static final Logger LOG = getLogger(AssetProcessor.class);

    private AssetScanner scanner;

    private MustacheContentLamabda lambda;

    public AssetProcessor() {
        this(new WebJarScanner());
    }

    public AssetProcessor(AssetScanner scanner) {
        this.scanner = scanner;
    }

    @Override
    public void init() {
        String content = getContent();
        if (!content.isEmpty()) {
            lambda = new MustacheContentLamabda(content);
        }
    }

    @Override
    public TemplateWrapper beforeExecute(String name, TemplateWrapper templateWrapper, Object context,
            Map<String, Object> parentContext) {
        if (lambda != null) {
            parentContext.put("assets", lambda);
        }
        return templateWrapper;
    }

    protected String getContent() {
        List<String> assets = scanner.scan(configuration);
        for (String asset : assets) {
            LOG.info(getString("AssetProcessor.found.asset"), new Object[] { asset });
        }
        StringBuilder builder = new StringBuilder();
        for (String asset : assets) {
            String resource = getResource(asset);
            if (resource != null) {
                builder.append(resource).append("\n");
            }
        }
        String content = builder.toString().trim();
        return content;
    }

    protected String getResource(String asset) {
        String resource = null;
        if (asset.endsWith(".js")) {
            resource = format("<script type=\"text/javascript\" src=\"%s\"></script>", getPath(asset));
        } else if (asset.endsWith(".css")) {
            resource = format("<link type=\"text/css\" rel=\"stylesheet\" href=\"%s\"></link>", getPath(asset));
        }
        return resource;
    }

    protected String getPath(String asset) {
        return asset;
    }
}
