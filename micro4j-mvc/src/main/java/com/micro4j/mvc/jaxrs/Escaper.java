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
package com.micro4j.mvc.jaxrs;

class Escaper {

    public static String escapeHtml(String content) {
        if (content == null) {
            return null;
        }
        int len = content.length();
        StringBuilder builder = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            char ascii = content.charAt(i);
            if (ascii == 60) {
                builder.append("&lt;");
            } else if (ascii == 62) {
                builder.append("&gt;");
            } else if (ascii == 38) {
                builder.append("&amp;");
            } else if (ascii == 39) {
                builder.append("&#39;");
            } else {
                builder.append(ascii);
            }
        }
        return builder.toString();
    }
}