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

import static java.lang.Integer.MAX_VALUE;
import static java.lang.Integer.compare;
import static java.lang.System.getProperty;
import static java.util.Collections.sort;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public abstract class AssetScanner {

    public static final int MIN_PRIORITY = MAX_VALUE;
    
    public static final int MAX_PRIORITY = 0;

    public List<String> scan() {
        String classpath = getProperty("java.class.path");
        String[] paths = classpath.split(";");
        return scan(paths);
    }

    protected abstract boolean isAsset(JarFile jar, JarEntry entry);

    public List<String> scan(String[] paths) {
        List<String> assets = new ArrayList<>();
        for (String path : paths) {
            File file = new File(path);
            if (!file.isFile()) {
                continue;
            }
            try (JarFile jar = new JarFile(path)) {
                Enumeration<JarEntry> entries = jar.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    if (entry.isDirectory() || !isAsset(jar, entry)) {
                        continue;
                    }
                    String name = toPath(entry);
                    name = removePrefix(name);
                    assets.add(name);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        sort(assets, (a1, a2) -> compare(getPriority(a1), getPriority(a2)));
        return assets;
    }

    protected boolean isJs(JarFile jar, JarEntry entry) {
        String name = entry.getName();
        return name.endsWith(".js") && !isMinJs(jar, entry);
    }

    protected boolean isMinJs(JarFile jar, JarEntry entry) {
        return entry.getName().endsWith(".min.js");
    }

    protected int getPriority(String path) {
        return MIN_PRIORITY;
    }

    protected String toPath(JarEntry entry) {
        return entry.getName();
    }

    protected String getPrefix() {
        return "META-INF/resources/";
    }

    protected String removePrefix(String path) {
        int begin = path.indexOf(getPrefix());
        if (begin >= 0) {
            return path.substring(getPrefix().length(), path.length());
        } else {
            return path;
        }
    }
}
