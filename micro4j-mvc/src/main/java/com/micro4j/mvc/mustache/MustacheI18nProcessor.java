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
package com.micro4j.mvc.mustache;

import static java.util.ResourceBundle.getBundle;

import static javax.ws.rs.core.HttpHeaders.*;
import java.util.List;
import java.util.Locale;
import java.util.Locale.LanguageRange;
import java.util.Map;
import java.util.ResourceBundle;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;

import com.micro4j.mvc.template.Processor;
import com.micro4j.mvc.template.TemplateWrapper;

import static java.util.Collections.emptyList;
import static java.util.Locale.lookup;
import static java.util.Locale.LanguageRange.parse;

public class MustacheI18nProcessor extends Processor {

    private String baseName;

    @Context
    private HttpHeaders headers;

    public MustacheI18nProcessor(String baseName) {
        this.baseName = baseName;
    }

    @Override
    public TemplateWrapper beforeExecute(String name, TemplateWrapper templateWrapper, Object context, Map<String, Object> parentContext) {
        List<Locale> languages = headers.getAcceptableLanguages();
        String acceptLanguage = headers.getHeaderString(ACCEPT_LANGUAGE);
        List<LanguageRange> ranges = emptyList();
        if (acceptLanguage != null && !acceptLanguage.trim().isEmpty()) {
            ranges = parse(acceptLanguage);
        }
        Locale locale = lookup(ranges, languages);
        if (locale == null) {
            locale = configuration.getDefaultLocale();
        }
        ResourceBundle bundle = getBundle(baseName, locale, configuration.getClassLoader());
        MustacheI18nLambda lambda = new MustacheI18nLambda(bundle);
        parentContext.put("i18n", lambda);
        return templateWrapper;
    }
}
