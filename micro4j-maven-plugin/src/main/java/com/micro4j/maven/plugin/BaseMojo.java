package com.micro4j.maven.plugin;

import static com.google.common.hash.Hashing.sha1;
import static java.lang.Math.abs;
import static java.lang.String.valueOf;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.copy;
import static java.nio.file.Files.createDirectory;
import static java.nio.file.Files.delete;
import static java.nio.file.Files.exists;
import static java.nio.file.Files.getLastModifiedTime;
import static java.nio.file.Files.isDirectory;
import static java.nio.file.Files.isReadable;
import static java.nio.file.Files.readAllBytes;
import static java.nio.file.Files.setLastModifiedTime;
import static java.nio.file.Files.size;
import static java.nio.file.Files.write;
import static java.nio.file.Paths.get;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.HashMap;
import java.util.Map;

import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.Scanner;
import org.sonatype.plexus.build.incremental.BuildContext;

public abstract class BaseMojo extends AbstractMojo {

    protected abstract void init() throws MojoExecutionException;

    protected abstract String getEncoding();

    protected abstract MavenProject getProject();

    protected abstract String[] getIncludes();

    protected abstract String[] getExcludes();

    protected abstract BuildContext getBuildContext();

    protected abstract String getOutputExtension();

    protected abstract String transform(Path srcFile, Path targetFile, String content) throws MojoExecutionException;

    @SuppressWarnings("serial")
    private static final Map<String, String> MAPPINGS = new HashMap<String, String>() {{
        put("jsx", "js");
        put("es6", "js");
        put("es7", "js");
        put("es" , "js");
    }};

    protected Map<String, String> getExtensionMappings() {
        return MAPPINGS;
    }

    protected boolean supportsExtensionRenaming() {
        return false;
    }

    protected void beforeProcess(Path srcOrgFile, Path targetOrgFile) throws MojoExecutionException {
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        init();
        MavenProject project = getProject();
        if (project.getResources() != null) {
            for (Resource resource : project.getResources()) {
                File srcDir = new File(resource.getDirectory());
                if (isDirectory(srcDir.toPath())) {
                    process(srcDir, project.getBuild().getOutputDirectory());
                }
            }
        }
        if (project.getTestResources() != null) {
            for (Resource resource : project.getTestResources()) {
                File srcDir = new File(resource.getDirectory());
                if (isDirectory(srcDir.toPath())) {
                    process(srcDir, project.getBuild().getTestOutputDirectory());
                }
            }
        }
    }

    protected void process(File srcDir, String targetDir) throws MojoExecutionException {
        boolean incremental = getBuildContext().isIncremental();
        boolean ignoreDelta = incremental ? false : true;
        Scanner scanner = getBuildContext().newScanner(srcDir, ignoreDelta);
        scanner.setIncludes(getIncludes());
        if (getExcludes() != null && getExcludes().length > 0) {
            scanner.setExcludes(getExcludes());
        }
        scanner.scan();
        for (String includedFile : scanner.getIncludedFiles()) {
            Path srcFile = srcDir.toPath().resolve(includedFile);
            Path srcOrgFile = srcDir.toPath().resolve(includedFile);
            Path targetOrgFile = Paths.get(targetDir).resolve(includedFile);
            Map<String, String> mappings = getExtensionMappings();
            if ( ! mappings.isEmpty() ) {
                String srcFileName = srcFile.getFileName().toString();
                int srcFileNameEnd = srcFileName.lastIndexOf(".");
                if (srcFileNameEnd > 0) {
                    String srcExtension = srcFileName.substring(srcFileNameEnd + 1, srcFileName.length());
                    String mappedExtension = mappings.get(srcExtension);
                    if (mappedExtension != null) {
                        srcFileName = srcFileName.substring(0, srcFileNameEnd) + "." + mappedExtension;
                        srcFile = srcFile.getParent().resolve(srcFileName);
                    }
                }
            }
            Path srcBaseDir = scanner.getBasedir().toPath();
            Path targetFile = new File(targetDir).toPath().resolve(srcBaseDir.relativize(srcFile));
            Path cacheDirectory = null;
            Path cacheFile = null;
            String hash = null;
            beforeProcess(srcOrgFile, targetOrgFile);
            try {
                if ( ! exists(targetOrgFile) ) {
                    copy(srcOrgFile, targetOrgFile);
                }
                if ( ! exists(targetFile) && exists(srcFile) ) {
                    copy(srcFile, targetFile);
                }
                if ( ! exists(targetFile) ) {
                    copy(srcOrgFile, targetFile);
                }
                
                String content = null;
                if ( ! getLastModifiedTime(srcOrgFile).equals(getLastModifiedTime(targetFile)) ||
                                supportsExtensionRenaming() && ! srcOrgFile.getFileName().equals(targetFile.getFileName()) ) {
                    content = new String(readAllBytes(srcOrgFile));
                } else {
                    content = new String(readAllBytes(targetFile));
                }
                cacheDirectory = getCacheDirectory();
                String modifiedContent = null;
                if (cacheDirectory != null) {
                    hash = getHash(content);
                    if ( hash != null && ! hash.trim().isEmpty() ) {
                        cacheFile = cacheDirectory.resolve(hash);
                        cacheFile = exists(cacheFile) &&
                                            isReadable(cacheFile) &&
                                            size(cacheFile) > 0 ?
                                            cacheFile : null;
                    }
                }
                if (cacheFile != null) {
                    FileTime cachedlm = getLastModifiedTime(cacheFile);
                    FileTime srcLm = getLastModifiedTime(srcOrgFile);
                    if ( ! cachedlm.equals(srcLm) ) {
                        delete(cacheFile);
                        cacheFile = null;
                    }
                }
                if ( cacheFile == null ) {
                    modifiedContent = transform(srcFile, targetFile, content);
                    if (modifiedContent != null) {
                        if ( getOutputExtension() != null && ! getOutputExtension().trim().isEmpty() ) {
                            String targetFileName = targetFile.getFileName().toString();
                            int targetFileNameEnd = targetFileName.lastIndexOf(".");
                            if (targetFileNameEnd > 0) {
                                targetFileName = targetFileName.substring(0, targetFileNameEnd) + "." + getOutputExtension();
                                targetFile = targetFile.getParent().resolve(targetFileName);
                            }
                        }
                        byte[] modified = modifiedContent.getBytes(getEncoding());
                        write(targetFile, modified, CREATE, TRUNCATE_EXISTING);
                        setLastModifiedTime(targetFile, getLastModifiedTime(srcOrgFile));
                        getBuildContext().refresh(targetFile.toFile());
                        if (cacheDirectory != null) {
                            cacheFile = cacheDirectory.resolve(hash);
                            if (size(targetFile) > 0) {
                                if ( exists(cacheFile) ) {
                                    delete(cacheFile);
                                }
                                copy(targetFile, cacheFile);
                                setLastModifiedTime(cacheFile, getLastModifiedTime(srcOrgFile));
                            }
                        }
                    }
                } else {
                    if ( exists(targetFile) ) {
                        delete(targetFile);
                    }
                    if (cacheFile != null) {
                        copy(cacheFile, targetFile);
                    }
                }
            } catch (Throwable e) {
                getLog().error(e);
                throw new MojoExecutionException(e.getMessage());
            }
        }
    }

    protected Path getCacheDirectory() {
        Path cache = null;
        String target = getProject().getBuild().getDirectory();
        if (target != null && ! target.trim().isEmpty() ) {
            Path targetPath = get(target);
            if ( exists(targetPath) ) {
                cache = targetPath.getParent().resolve("micro4j-cache");
                if ( ! exists(cache) ) {
                    try {
                        createDirectory(cache);
                    } catch (IOException e) {
                        getLog().warn(e);
                        return null;
                    }
                }
            }
        }
        return cache;
    }

    protected String getHash(String content) {
        return valueOf(abs(sha1()
                        .newHasher()
                        .putString(content, UTF_8)
                        .hash()
                        .asLong())
                ) + "-" + getClass().getSimpleName();
    }
}
