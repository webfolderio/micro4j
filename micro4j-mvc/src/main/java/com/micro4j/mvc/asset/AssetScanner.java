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
import static java.nio.file.Files.isRegularFile;
import static java.nio.file.Files.walk;
import static java.util.Collections.sort;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import com.micro4j.mvc.Configuration;
import com.micro4j.mvc.MvcException;

import static java.lang.String.format;

public abstract class AssetScanner {

    public static final int MIN_PRIORITY = MAX_VALUE;

    public static final int MAX_PRIORITY = 0;

    protected abstract boolean isAsset(String name);

    public List<String> scan(Configuration configuration) {
        return scan(configuration.getClassLoader());
    }

    public List<String> scan(ClassLoader classLoader) {
        List<String> assets = new ArrayList<>();
        try {
            Enumeration<URL> enumeration = classLoader.getResources("META-INF/resources");
            while (enumeration.hasMoreElements()) {
                URL url = enumeration.nextElement();
                URLConnection urlConnection = url.openConnection();
                if ((urlConnection instanceof JarURLConnection)) {
                    JarURLConnection connection = (JarURLConnection) urlConnection;
                    try (JarFile jar = connection.getJarFile()) {
                        Enumeration<JarEntry> entries = jar.entries();
                        while (entries.hasMoreElements()) {
                            JarEntry entry = entries.nextElement();
                            if (entry.isDirectory() || !isAsset(entry.getName())) {
                                continue;
                            }
                            String name = removePrefix(entry.getName());
                            assets.add(name);
                        }
                    }
                } else {
                    String protocol = url.getProtocol();
                    if (!"file".equals(protocol)) {
                        continue;
                    }
                    File directory = new File(url.getFile());
                    if (!directory.isDirectory()) {
                        continue;
                    }
                    Path path = directory.toPath();
                    Iterator<Path> iter = walk(path).iterator();
                    while (iter.hasNext()) {
                        Path next = iter.next();
                        if (isRegularFile(next)) {
                            String name = path.relativize(next).toString().replace('\\', '/');
                            String location = format("%s/%s/%s", "META-INF", "resources", name);
                            if (isAsset(location)) {
                                assets.add(name);
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new MvcException(e);
        }
        sort(assets, (a1, a2) -> compare(getPriority(a1), getPriority(a2)));
        return assets;
    }

    protected boolean isCss(String path) {
        return path.endsWith(".css") && !isMinCss(path);
    }

    protected boolean isMinCss(String path) {
        return path.endsWith(".min.css");
    }

    protected boolean isJs(String path) {
        return path.endsWith(".js") && !isMinJs(path);
    }

    protected boolean isMinJs(String path) {
        return path.endsWith(".min.js");
    }

    protected int getPriority(String path) {
        if (isCss(path)) {
            return MAX_PRIORITY;
        }
        return MIN_PRIORITY;
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
