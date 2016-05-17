package com.micro4j.sample.mvc.pjax;

import java.util.List;
import java.util.Map;

import com.micro4j.mvc.asset.WebJarJavascriptScanner;
import com.micro4j.mvc.template.Processor;

import static java.util.Collections.emptyList;

public class AssetProcessor extends Processor {

    private List<String> assets = emptyList();

    public AssetProcessor() {
        assets = new WebJarJavascriptScanner().scan();
    }

    public String afterExecute(String name, String content, Object context, Map<String, Object> parentContext) {
        parentContext.put("assets", assets);
        return content;
    }
}
