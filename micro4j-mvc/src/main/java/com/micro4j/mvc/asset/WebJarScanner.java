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
package com.micro4j.mvc.asset;

public class WebJarScanner extends AssetScanner {

    public static final int JQUERY = MAX_PRIORITY + 1000;

    public static final int INCREMENTAL_DOM = JQUERY + 1;

    public static final int HTML2INCDOM = INCREMENTAL_DOM + 1;

    @Override
    protected boolean isAsset(String name) {
        return (isJs(name) && !isRequireJs(name)) || isCss(name);
    }

    protected boolean isRequireJs(String name) {
        return name.endsWith("webjars-requirejs.js");
    }

    @Override
    protected int getPriority(String path) {
        if (path.startsWith("webjars/jquery/")) {
            return JQUERY;
        } else if (path.startsWith("webjars/incremental-dom/")) {
            return INCREMENTAL_DOM;
        } else if (path.startsWith("webjars/html2incdom/")) {
            return HTML2INCDOM;
        } else {
            return super.getPriority(path);
        }
    }
}
