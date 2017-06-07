/**
 * The MIT License
 * Copyright © 2016 - 2017 WebFolder OÜ
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package io.webfolder.micro4j.mvc.template;

import static io.webfolder.micro4j.mvc.MvcMessages.getString;
import static java.nio.file.Files.isDirectory;
import static java.nio.file.Files.isReadable;
import static java.nio.file.Files.newInputStream;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;

import io.webfolder.micro4j.mvc.Configuration;

public class LocalLoader extends AbstractContentLoader {

    private static final Logger LOG = getLogger(LocalLoader.class);

    private List<Path> directories = emptyList();

    public LocalLoader(Configuration configuration, Set<Path> paths) {
        super(configuration);
        if (paths == null || paths.isEmpty()) {
            LOG.error(getString("LocalLoader.empty.dir.list")); //$NON-NLS-1$
            throw new IllegalArgumentException("paths");
        }
        List<Path> directories = new ArrayList<>();
        for (Path next : paths) {
            if (isDirectory(next)) {
                LOG.info(getString("LocalLoader.valid.directory"), //$NON-NLS-1$
                        new Object[] { next.toString() });
                directories.add(next.toAbsolutePath().normalize());
                continue;
            } else {
                LOG.error(getString("LocalLoader.path.is.not.valid"), //$NON-NLS-1$
                        new Object[] { next.toString() });
            }
        }
        this.directories = unmodifiableList(directories);
    }

    @Override
    public InputStream getContent(String name) {
        Path path = toPath(name);
        if (path != null) {
            try {
                return newInputStream(path);
            } catch (IOException e) {
                LOG.error(e.getMessage(), e);
            }
        }
        return null;
    }

    protected Path toPath(String name) {
        for (Path directory : directories) {
            Path path = directory.resolve(configuration.getPrefix()).resolve(name);
            if (isReadable(path)) {
                return path;
            }
        }
        return null;
    }
}
