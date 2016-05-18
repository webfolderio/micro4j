package com.micro4j.mvc.test;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import com.micro4j.mvc.Configuration;
import com.micro4j.mvc.asset.AssetScanner;
import com.micro4j.mvc.asset.WebJarScanner;

public class AssetScannerTest {

    @Test
    public void testWebJarJsScanner() {
        AssetScanner scanner = new WebJarScanner() {

            @Override
            protected int getPriority(String path) {
                if ("webjars/lib.js".equals(path)) {
                    return MAX_PRIORITY + 1;
                }
                return super.getPriority(path);
            }

            @Override
            protected boolean isAsset(String path) {
                if (path.contains("bootstrap") && !path.endsWith("bootstrap.css")) {
                    return false;
                }
                boolean asset = (isJs(path) && !isRequireJs(path)) || isCss(path);
                return asset;
            }
        };
        List<String> assets = scanner.scan(new Configuration.Builder().build());
        assertEquals(4, assets.size());
        assertEquals("webjars/bootstrap/3.3.6/css/bootstrap.css", assets.get(0));
        assertEquals("webjars/lib.js", assets.get(1));
        assertEquals("webjars/jquery/1.11.1/jquery.js", assets.get(2));
        assertEquals("webjars/jquery-pjax/1.9.6/jquery.pjax.js", assets.get(3));
    }
}
