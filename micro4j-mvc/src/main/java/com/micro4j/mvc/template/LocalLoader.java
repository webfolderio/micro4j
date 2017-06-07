package com.micro4j.mvc.template;

import static com.micro4j.mvc.MvcMessages.getString;
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

import com.micro4j.mvc.Configuration;

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
