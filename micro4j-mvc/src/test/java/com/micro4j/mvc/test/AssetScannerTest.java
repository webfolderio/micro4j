package com.micro4j.mvc.test;

import java.util.List;

import org.junit.Test;

import com.micro4j.mvc.asset.AssetScanner;
import com.micro4j.mvc.asset.WebJarJavascriptScanner;

import static org.junit.Assert.*;

public class AssetScannerTest {

    @Test
    public void testWebJarJsScanner() {
        AssetScanner scanner = new WebJarJavascriptScanner();
        List<String> assets = scanner.scan();
        assertEquals(2, assets.size());
        assertEquals("webjars/jquery/1.11.1/jquery.js", assets.get(0));
        assertEquals("webjars/jquery-pjax/1.9.6/jquery.pjax.js", assets.get(1));
    }
}
