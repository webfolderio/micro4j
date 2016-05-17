package com.micro4j.mvc.asset;

import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class WebJarJavascriptScanner extends AssetScanner {

    @Override
    protected boolean isAsset(JarFile jar, JarEntry entry) {
        return isJs(jar, entry) && !isRequireJs(jar, entry);
    }

    protected boolean isRequireJs(JarFile jar, JarEntry entry) {
        return entry.getName().endsWith("webjars-requirejs.js");
    }

    @Override
    protected int getPriority(String path) {
        return path.startsWith("webjars/jquery/") ?
                        MAX_PRIORITY :
                        MIN_PRIORITY;
    }
}
