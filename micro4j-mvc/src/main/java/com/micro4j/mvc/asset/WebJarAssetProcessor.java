package com.micro4j.mvc.asset;

import java.util.List;
import java.util.Map;

import com.micro4j.mvc.asset.WebJarJavascriptScanner;
import com.micro4j.mvc.template.Processor;
import com.micro4j.mvc.template.TemplateWrapper;

import static java.util.Collections.emptyList;

public class WebJarAssetProcessor extends Processor {

    private List<String> assets = emptyList();

    public WebJarAssetProcessor() {
        assets = new WebJarJavascriptScanner().scan();
    }

    @Override
    public TemplateWrapper beforeExecute(String name, TemplateWrapper templateWrapper, Object context,
            Map<String, Object> parentContext) {
        parentContext.put("assets", assets);
        return templateWrapper;
    }
}
