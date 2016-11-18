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
package com.micro4j.maven.plugin;

import static java.nio.file.Files.delete;
import static java.nio.file.Files.exists;
import static java.nio.file.Files.getLastModifiedTime;
import static java.nio.file.Files.move;
import static java.nio.file.Files.setLastModifiedTime;
import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static org.apache.maven.plugins.annotations.LifecyclePhase.PREPARE_PACKAGE;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

@Mojo(name = "move", defaultPhase = PREPARE_PACKAGE, threadSafe = true, requiresOnline = false, requiresReports = false)
public class MoveMojo extends AbstractMojo {

    @Parameter(required = true)
    private File sourceFile;

    @Parameter(required = true)
    private File destinationFile;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (sourceFile == null || destinationFile == null) {
            return;
        }
        Path src = sourceFile.toPath();
        Path dest = destinationFile.toPath();
        try {
            if (exists(src)) {
                FileTime srcLm = getLastModifiedTime(src);
                if (exists(dest)) {
                    FileTime destLm = getLastModifiedTime(dest);
                    if ( ! srcLm.equals(destLm) ) {
                        delete(dest);
                        move(src, dest, ATOMIC_MOVE);
                        setLastModifiedTime(dest, srcLm);
                    }
                } else {
                    move(src, dest, ATOMIC_MOVE);
                    setLastModifiedTime(dest, srcLm);
                }
            }
        } catch (IOException e) {
            getLog().error(e.getMessage(), e);
            throw new MojoExecutionException(e.getMessage());
        }
    }
}
