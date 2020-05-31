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

import static java.util.Collections.emptyList;
import static java.util.Locale.lookup;
import static java.util.Locale.LanguageRange.parse;
import static java.util.ResourceBundle.getBundle;
import static javax.ws.rs.core.HttpHeaders.ACCEPT_LANGUAGE;

import java.util.List;
import java.util.Locale;
import java.util.Locale.LanguageRange;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;

import io.webfolder.micro4j.mvc.mustachejava.MustacheI18nLambda;
import io.webfolder.micro4j.mvc.template.TemplateIntereceptor;
import io.webfolder.micro4j.mvc.template.TemplateWrapper;

public class MustacheI18nInterceptor extends TemplateIntereceptor {

    private final String baseName;

    @Context
    private HttpHeaders headers;

    @Context
    private UriInfo uriInfo;

    private final ConcurrentMap<Locale, MustacheI18nLambda> lambdas = new ConcurrentHashMap<>();

    private final Map<String, Locale> locales = new ConcurrentHashMap<>();

    public MustacheI18nInterceptor(String baseName) {
        this.baseName = baseName;
    }

    @Override
    public TemplateWrapper beforeExecute(String name, TemplateWrapper templateWrapper, Object context, Map<String, Object> parentContext) {
        Locale locale = getPreferedLocale();
        if (locale == null) {
            locale = getRequestLocale();
        }
        if (locale == null) {
            locale = configuration.getLocale();
        }
        ResourceBundle bundle = getResourceBundle(locale);
        MustacheI18nLambda lambda = null;
        if (configuration.isEnableTemplateCaching()) {
            lambda = lambdas.computeIfAbsent(locale, (l) -> new MustacheI18nLambda(bundle));
        } else {
            lambda = new MustacheI18nLambda(bundle);
        }
        parentContext.put("i18n", lambda);
        return templateWrapper;
    }

    protected ResourceBundle getResourceBundle(Locale locale) {
        ResourceBundle bundle = getBundle(baseName, locale, configuration.getClassLoader());
        return bundle;
    }

    protected Locale getRequestLocale() {
        List<Locale> languages = getHttpHeaders().getAcceptableLanguages();
        String acceptLanguage = getHttpHeaders().getHeaderString(ACCEPT_LANGUAGE);
        List<LanguageRange> ranges = emptyList();
        if (acceptLanguage != null && !acceptLanguage.trim().isEmpty()) {
            ranges = parse(acceptLanguage);
        }
        Locale locale = lookup(ranges, languages);
        return locale;
    }

    protected Locale getPreferedLocale() {
        MultivaluedMap<String,String> parameters = getUriInfo().getQueryParameters();
        if (parameters.isEmpty()) {
            return null;
        }
        String lang = parameters.getFirst(getPreferedLangParam());
        if (lang == null || lang.trim().isEmpty()) {
            return null;
        }
        Locale locale = locales.get(lang);
        if (locale != null) {
            return locale;
        }
        locale = new Locale(lang);
        locales.put(lang, locale);
        return locale;
    }

    protected HttpHeaders getHttpHeaders() {
        return headers;
    }

    protected UriInfo getUriInfo() {
        return uriInfo;
    }

    protected String getPreferedLangParam() {
        return "lang";
    }
}
