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

    private String prefix;

    public static final int MIN_PRIORITY = MAX_VALUE;
    
    public static final int MAX_PRIORITY = 0;

    public AssetScanner(String prefix) {
        this.prefix = prefix;
    }

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

    protected String removePrefix(String path) {
        int begin = path.indexOf(prefix);
        if (begin >= 0) {
            return path.substring(prefix.length(), path.length());
        } else {
            return path;
        }
    }
}
