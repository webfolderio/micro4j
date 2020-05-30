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
import static java.util.Collections.emptyMap;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;

import io.webfolder.micro4j.mvc.Configuration;
import io.webfolder.micro4j.mvc.MvcException;

public abstract class AbstractTemplateEngine implements TemplateEngine {

    private static final Logger LOG = getLogger(TemplateEngine.class);

    private Map<String, TemplateWrapper> cache = emptyMap();

    private ContentLoader contentLodaer;

    private Configuration configuration;

    public abstract TemplateWrapper compile(String name, String content);

    public AbstractTemplateEngine(Configuration configuration, ContentLoader contentLoader) {
        this.configuration = configuration;
        this.contentLodaer = contentLoader;
        if (configuration.isEnableTemplateCaching()) {
            cache = new ConcurrentHashMap<>();
        }
    }

    @Override
    public void execute(String name, Object context, Map<String, Object> parentContext, Writer writer) {
        TemplateWrapper template = cache.get(name);
        if (template == null) {
            String content = contentLodaer.get(name);
            for (TemplateIntereceptor intereceptor : configuration.getInterceptors()) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug(getString("AbstractTemplateEngine.beforeCompile"), //$NON-NLS-1$
                            new Object[] { intereceptor.getClass().getName() });
                }
                content = intereceptor.beforeCompile(name, content);
            }
            LOG.info(getString("AbstractTemplateEngine.compiling"), new Object[] { //$NON-NLS-1$
                    name });
            template = compile(name, content);
            for (TemplateIntereceptor interceptor : configuration.getInterceptors()) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug(getString("AbstractTemplateEngine.afterCompile"), //$NON-NLS-1$
                            new Object[] { interceptor.getClass().getSimpleName() });
                }
                template = interceptor.afterCompile(template);
            }
            if (configuration.isEnableTemplateCaching()) {
                TemplateWrapper existingTemplate = cache.putIfAbsent(name, template);
                if (existingTemplate != null) {
                    template = existingTemplate;
                }
            }
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug(getString("AbstractTemplateEngine.executing.template"), new Object[] { //$NON-NLS-1$
                    name, context, parentContext });
        }
        for (TemplateIntereceptor interceptor : configuration.getInterceptors()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug(getString("AbstractTemplateEngine.beforeExecute"), //$NON-NLS-1$
                        new Object[] { interceptor.getClass().getSimpleName() });
            }
            template = interceptor.beforeExecute(name, template, context, parentContext);
        }
        StringWriter buffer = new StringWriter();
        template.execute(context, parentContext, buffer);
        String content = buffer.toString();
        for (TemplateIntereceptor interceptor : configuration.getInterceptors()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug(getString("AbstractTemplateEngine.afterExecute"), //$NON-NLS-1$
                        new Object[] { interceptor.getClass().getSimpleName() });
            }
            content = interceptor.afterExecute(name, content, context, parentContext);
        }
        try (Writer wr = writer) {
            wr.write(content);
        } catch (IOException e) {
            throw new MvcException(e);
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug(getString("AbstractTemplateEngine.execution.done"), new Object[] { //$NON-NLS-1$
                    name });
        }
    }

    @Override
    public Configuration getConfiguration() {
        return configuration;
    }

    @Override
    public ContentLoader getContentLoader() {
        return contentLodaer;
    }
}
