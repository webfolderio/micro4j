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
        AssetScanner scanner = new WebJarScanner(new Configuration.Builder().build()) {

            @Override
            protected int getPriority(String path) {
                if ("webjars/lib.js".equals(path)) {
                    return MAX_PRIORITY + 1;
                }
                return super.getPriority(path);
            }
        };
        List<String> assets = scanner.scan();
        assertEquals(3, assets.size());
        assertEquals("webjars/jquery/1.11.1/jquery.js", assets.get(0));
        assertEquals("webjars/lib.js", assets.get(1));
        assertEquals("webjars/jquery-pjax/1.9.6/jquery.pjax.js", assets.get(2));
    }
}
