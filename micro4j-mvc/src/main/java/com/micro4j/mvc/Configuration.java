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
package com.micro4j.mvc;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableSet;
import static java.util.Locale.getDefault;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.micro4j.mvc.mustache.MustacheFormatter;
import com.micro4j.mvc.template.DefaultFormatter;
import com.micro4j.mvc.template.Processor;

public final class Configuration {

    private Set<String> fileTypeExtensions;

    private List<Processor> pocessors;

    private String bodyName;

    private ClassLoader classLoader;

    private Charset charset;

    private DefaultFormatter formatter;

    private String prefix;

    private String defaultContainer;

    private boolean enableTemplateCaching;

    private String defaultNullValue;

    private Locale defaultLocale;

    public static class Builder {

        private String defaultContainer = "";

        private String prefix = "";

        private String defaultNullValue = "";

        private Charset charset = UTF_8;

        private String bodyName = "body";

        private Locale defaultLocale = getDefault();

        private boolean enableTemplateCaching = true;

        private ClassLoader classLoader = Configuration.class.getClassLoader();

        private Set<String> fileTypeExtensions = new HashSet<>(asList("html"));

        private List<Processor> processors = new ArrayList<>();

        private DefaultFormatter formatter = new MustacheFormatter();

        public Builder prefix(String prefix) {
            this.prefix = prefix;
            return this;
        }

        public Builder formatter(DefaultFormatter formatter) {
            this.formatter = formatter;
            return this;
        }

        public Builder defaultContainer(String defaultContainer) {
            if (defaultContainer == null) {
                throw new IllegalArgumentException("defaultContainer");
            }
            this.defaultContainer = defaultContainer;
            return this;
        }

        public Builder fileTypeExtensions(String... fileTypeExtensions) {
            if (fileTypeExtensions == null || fileTypeExtensions.length <= 0) {
                throw new IllegalArgumentException("fileTypeExtensions");
            }
            Set<String> set = new HashSet<>();
            for (String next : fileTypeExtensions) {
                set.add(next);
            }
            this.fileTypeExtensions = set;
            return this;
        }

        public Builder processors(Processor... processors) {
            if (processors == null || processors.length <= 0) {
                throw new IllegalArgumentException("processors");
            }
            this.processors.addAll(asList(processors));
            return this;
        }

        public Builder bodyName(String bodyName) {
            if (bodyName == null || bodyName.trim().isEmpty()) {
                throw new IllegalArgumentException("bodyName");
            }
            this.bodyName = bodyName;
            return this;
        }

        public Builder classLoader(ClassLoader classLoader) {
            if (classLoader == null) {
                throw new IllegalArgumentException("classLoader");
            }
            this.classLoader = classLoader;
            return this;
        }

        public Builder charset(Charset charset) {
            if (charset == null) {
                throw new IllegalArgumentException("charset");
            }
            this.charset = charset;
            return this;
        }

        public Builder defaultLocale(Locale defaultLocale) {
            if (defaultLocale == null) {
                throw new IllegalArgumentException("defaultLocale");
            }
            this.defaultLocale = defaultLocale;
            return this;
        }

        public Builder defaultNullValue(String defaultNullValue) {
            this.defaultNullValue = defaultNullValue;
            return this;
        }

        public Builder enableTemplateCaching(boolean enableTemplateCaching) {
            this.enableTemplateCaching = enableTemplateCaching;
            return this;
        }

        public Configuration build() {
            Configuration configuration = new Configuration();
            configuration.bodyName = bodyName;
            configuration.defaultContainer = defaultContainer;
            configuration.fileTypeExtensions = unmodifiableSet(fileTypeExtensions);
            configuration.formatter = formatter;
            configuration.prefix = prefix;
            configuration.charset = charset;
            configuration.classLoader = classLoader;
            configuration.defaultNullValue = defaultNullValue;
            configuration.enableTemplateCaching = enableTemplateCaching;
            configuration.defaultLocale = defaultLocale;
            for (Processor processor : processors) {
                processor.setConfiguration(configuration);
            }
            configuration.pocessors = unmodifiableList(processors);
            return configuration;
        }
    }

    private Configuration() {
    }

    public Set<String> getExtensions() {
        return fileTypeExtensions;
    }

    public String getPrefix() {
        return prefix;
    }

    public DefaultFormatter getFormatter() {
        return formatter;
    }

    public String defaultNullValue() {
        return defaultNullValue;
    }

    public String getDefaultContainer() {
        return defaultContainer;
    }

    public void getContainerName(String containerName) {
        this.bodyName = containerName;
    }

    public String getBodyName() {
        return bodyName;
    }

    public List<Processor> getProcessors() {
        return pocessors;
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    public Charset getCharset() {
        return charset;
    }

    public boolean isEnableTemplateCaching() {
        return enableTemplateCaching;
    }

    public Locale getDefaultLocale() {
        return defaultLocale;
    }

    @Override
    public String toString() {
        return "Configuration [fileTypeExtensions=" + fileTypeExtensions + ", pocessors=" + pocessors + ", bodyName="
                + bodyName + ", classLoader=" + classLoader + ", charset=" + charset + ", formatter=" + formatter
                + ", prefix=" + prefix + ", defaultContainer=" + defaultContainer + ", enableTemplateCaching="
                + enableTemplateCaching + ", defaultNullValue=" + defaultNullValue + ", defaultLocale=" + defaultLocale
                + "]";
    }
}
