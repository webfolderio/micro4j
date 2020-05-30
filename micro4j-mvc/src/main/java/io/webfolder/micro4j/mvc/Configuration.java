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
package io.webfolder.micro4j.mvc;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableSet;
import static java.util.Locale.getDefault;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import io.webfolder.micro4j.mvc.mustache.MustacheFormatter;
import io.webfolder.micro4j.mvc.template.DefaultFormatter;
import io.webfolder.micro4j.mvc.template.TemplateIntereceptor;

public class Configuration {

    private Set<String> fileTypeExtensions;

    private List<TemplateIntereceptor> interceptors;

    private String bodyName;

    private ClassLoader classLoader;

    private Charset charset;

    private DefaultFormatter formatter;

    private String prefix;

    private String container;

    private boolean enableTemplateCaching;

    private String nullValue;

    private Locale locale;

    private String delims;

    private boolean esacepHtml;

    private Set<String> escapeHtmlExcludes = emptySet();

    public static class Builder {

        private String container = "";

        private String prefix = "";

        private String nullValue = "";

        private Charset charset = UTF_8;

        private String delims = "{{ }}";

        private String bodyName = "body";

        private Locale locale = getDefault();

        private boolean enableTemplateCaching = true;

        private ClassLoader classLoader = Configuration.class.getClassLoader();

        private Set<String> fileTypeExtensions = new HashSet<>(asList("html"));

        private List<TemplateIntereceptor> interceptors = new ArrayList<>();

        private Set<String> escapeHtmlExcludes = emptySet();

        private DefaultFormatter formatter = new MustacheFormatter();

        private boolean escapeHtml;

        public Builder prefix(String prefix) {
            this.prefix = prefix;
            return this;
        }

        public Builder formatter(DefaultFormatter formatter) {
            this.formatter = formatter;
            return this;
        }

        public Builder escapeHtml(boolean esacepHtml) {
            this.escapeHtml = esacepHtml;
            return this;
        }

        public Builder container(String container) {
            if (container == null) {
                throw new IllegalArgumentException("container");
            }
            this.container = container;
            return this;
        }

        public Builder escapeHtmlExcludes(Set<String> escapeHtmlExcludes) {
            if (escapeHtmlExcludes == null || escapeHtmlExcludes.size() <= 0) {
                throw new IllegalArgumentException("escapeHtmlExcludes");
            }
            this.escapeHtmlExcludes = escapeHtmlExcludes;
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

        public Builder interceptors(TemplateIntereceptor... interceptors) {
            if (interceptors == null || interceptors.length <= 0) {
                throw new IllegalArgumentException("interceptors");
            }
            this.interceptors.addAll(asList(interceptors));
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

        public Builder locale(Locale locale) {
            if (locale == null) {
                throw new IllegalArgumentException("locale");
            }
            this.locale = locale;
            return this;
        }

        public Builder nullValue(String nullValue) {
            this.nullValue = nullValue;
            return this;
        }

        public Builder delims(String delims) {
            if (delims == null || delims.trim().isEmpty()) {
                throw new IllegalArgumentException("delims");
            }
            this.delims = delims;
            return this;
        }

        public Builder enableTemplateCaching(boolean enableTemplateCaching) {
            this.enableTemplateCaching = enableTemplateCaching;
            return this;
        }

        public Configuration build() {
            Configuration configuration = new Configuration();
            configuration.bodyName = bodyName;
            configuration.container = container;
            configuration.fileTypeExtensions = unmodifiableSet(fileTypeExtensions);
            configuration.formatter = formatter;
            configuration.prefix = prefix;
            configuration.charset = charset;
            configuration.classLoader = classLoader;
            configuration.nullValue = nullValue;
            configuration.enableTemplateCaching = enableTemplateCaching;
            configuration.locale = locale;
            configuration.delims = delims;
            configuration.esacepHtml = escapeHtml;
            configuration.escapeHtmlExcludes = escapeHtmlExcludes;
            for (TemplateIntereceptor interceptor : interceptors) {
                interceptor.setConfiguration(configuration);
            }
            configuration.interceptors = unmodifiableList(interceptors);
            return configuration;
        }
    }

    private Configuration() {
    }

    public static Configuration defaultConfiguration() {
        return new Builder().build();
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

    public String getNullValue() {
        return nullValue;
    }

    public String getContainer() {
        return container;
    }

    public String getBodyName() {
        return bodyName;
    }

    public List<TemplateIntereceptor> getInterceptors() {
        return interceptors;
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    public Charset getCharset() {
        return charset;
    }

    public Set<String> escapeHtmlExcludes() {
        return escapeHtmlExcludes;
    }

    public boolean isEnableTemplateCaching() {
        return enableTemplateCaching;
    }

    public Locale getLocale() {
        return locale;
    }

    public String getDelims() {
        return delims;
    }

    public boolean isEsacepHtml() {
        return esacepHtml;
    }

    @Override
    public String toString() {
        return "Configuration [fileTypeExtensions=" + fileTypeExtensions + ", interceptors=" + interceptors
                + ", bodyName=" + bodyName + ", classLoader=" + classLoader + ", charset=" + charset + ", formatter="
                + formatter + ", prefix=" + prefix + ", container=" + container + ", enableTemplateCaching="
                + enableTemplateCaching + ", nullValue=" + nullValue + ", locale=" + locale + ", delims=" + delims
                + ", esacepHtml=" + esacepHtml + "]";
    }
}
