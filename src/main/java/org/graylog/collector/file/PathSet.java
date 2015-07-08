/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog.collector.file;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Set;

public class PathSet {
    private static final Logger LOG = LoggerFactory.getLogger(PathSet.class);

    // List taken from sun.nio.fs.Globs.
    private static final List<String> GLOB_META_CHARS = ImmutableList.of("\\", "*", "?", "[", "{");

    private final String pattern;
    private final FileTreeWalker fileTreeWalker;
    private final PathMatcher matcher;
    private final Path rootPath;

    public static class GlobbingFileVisitor extends SimpleFileVisitor<Path> {
        private final PathMatcher matcher;
        private final ImmutableSet.Builder<Path> matchedPaths;

        public GlobbingFileVisitor(PathMatcher matcher, ImmutableSet.Builder<Path> matchedPaths) {
            this.matcher = matcher;
            this.matchedPaths = matchedPaths;
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
            // Skip /proc because it can throw some permission errors we cannot check for.
            return dir.startsWith("/proc") ? FileVisitResult.SKIP_SUBTREE : FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
            LOG.warn("Unable to change into directory {} - Check permissions", file);
            return FileVisitResult.SKIP_SUBTREE;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
            if (matcher.matches(file)) {
                // TODO needs to be an absolute path because otherwise the FileObserver does weird things. Investigate what's wrong with it.
                matchedPaths.add(file.toAbsolutePath());
            }

            return FileVisitResult.CONTINUE;
        }
    }

    public interface FileTreeWalker {
        void walk(Path basePath, FileVisitor<Path> visitor) throws IOException;
    }

    /**
     * Default FileTrackingList that uses the file system to scan for files to track.
     *
     * @param pattern the pattern to scan for
     */
    public PathSet(final String pattern) {
        this(pattern, new FileTreeWalker() {
            @Override
            public void walk(Path basePath, FileVisitor<Path> visitor) throws IOException {
                Files.walkFileTree(basePath, visitor);
            }
        });
    }

    public PathSet(final String pattern, FileTreeWalker fileTreeWalker) {
        this.pattern = pattern;
        this.fileTreeWalker = fileTreeWalker;
        this.matcher = FileSystems.getDefault().getPathMatcher("glob:" + pattern);
        this.rootPath = getRootPathFromPattern(pattern);
    }

    public Path getRootPath() {
        return rootPath;
    }

    public Set<Path> getPaths() throws IOException {
        final ImmutableSet.Builder<Path> matchedPaths = ImmutableSet.builder();

        if (hasGlobMetaChars(pattern)) {
            fileTreeWalker.walk(rootPath, new GlobbingFileVisitor(matcher, matchedPaths));
        } else {
            // TODO needs to be an absolute path because otherwise the FileObserver does weird things. Investigate what's wrong with it.
            matchedPaths.add(Paths.get(pattern).toAbsolutePath());
        }

        return matchedPaths.build();
    }

    private Path getRootPathFromPattern(String pattern) {
        if (!hasGlobMetaChars(pattern)) {
            return Paths.get(pattern).toAbsolutePath().getParent();
        }

        final List<String> elements = Lists.newArrayList();

        for (Path name: Paths.get(pattern)) {
            if (hasGlobMetaChars(name.toString())) {
                break;
            }

            elements.add(name.toString());
        }

        return Paths.get("/", elements.toArray(new String[elements.size()]));
    }

    private boolean hasGlobMetaChars(String path) {
        if (path == null) {
            return false;
        }
        for (String globMetaChar : GLOB_META_CHARS) {
            if (path.contains(globMetaChar)) {
                return true;
            }
        }
        return false;
    }
}