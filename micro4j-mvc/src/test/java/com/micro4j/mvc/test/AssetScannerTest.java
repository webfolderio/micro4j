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
