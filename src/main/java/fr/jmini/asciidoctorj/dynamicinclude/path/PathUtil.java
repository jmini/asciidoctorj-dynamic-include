/**
 *
 */
package fr.jmini.asciidoctorj.dynamicinclude.path;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.yaml.snakeyaml.Yaml;

/**
 * @author jbr
 *
 */
public class PathUtil {
    private static final String STAR_REPLACEMENT = "__STAR__";

    public static List<Path> findFiles(Path dir, String glob, List<String> nameSuffixes) {
        Path normalizedGlob = dir.resolve(sanitizeStringPath(glob))
                .normalize();
        final PathMatcher matcher = FileSystems.getDefault()
                .getPathMatcher("glob:" + unsanitizeStringPath(normalizePath(normalizedGlob)
                        .replace('\\', '/')));

        List<Path> result = new ArrayList<>();
        try {
            Path walkRoot = findWalkRoot(normalizedGlob);
            Files.walkFileTree(walkRoot, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (matcher.matches(file)) {
                        String fileName = file.toFile()
                                .getName();
                        String nameSuffix = getNameSuffix(fileName);
                        if (nameSuffix != null) {
                            if (nameSuffixes.contains(nameSuffix)) {
                                result.add(file.toAbsolutePath()
                                        .normalize());
                            }
                        } else {
                            result.add(file.toAbsolutePath()
                                    .normalize());
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            //TODO: do something else with the exception
            e.printStackTrace();
        }
        return Collections.unmodifiableList(result);
    }

    public static List<Path> sortFiles(Consumer<String> logger, List<Path> list) {
        AbsolutePathComparator comparator = new AbsolutePathComparator(PathUtil::loadPageOrder);
        List<Path> result = list.stream()
                .sorted(comparator)
                .collect(Collectors.toList());
        comparator.getMessages()
                .stream()
                .sorted()
                .forEach(logger::accept);
        return result;
    }

    static List<String> loadPageOrder(Path path) {
        if (Files.isDirectory(path)) {
            Path yamlFile = path.resolve("pages.yaml");
            if (Files.isReadable(yamlFile)) {
                Yaml yaml = new Yaml();
                try (InputStream inputStream = Files.newInputStream(yamlFile)) {
                    Pages pages = yaml.loadAs(inputStream, Pages.class);
                    return pages.getOrder();
                } catch (IOException e) {
                    //TODO do something with this exception
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    static Path getCommonPath(Path p1, Path p2) {
        if (Objects.equals(p1, p2)) {
            return p1;
        }

        Path result = null;
        if (p1.isAbsolute() && p2.isAbsolute() && Objects.equals(p1.getRoot(), p2.getRoot())) {
            result = p1.getRoot();
        } else if (!p1.isAbsolute() && !p2.isAbsolute()) {
            result = Paths.get("");
        }

        if (result != null) {
            Iterator<Path> i1 = p1.iterator();
            Iterator<Path> i2 = p2.iterator();
            Path path1 = null;
            Path path2 = null;
            while (i1.hasNext() && i2.hasNext() && Objects.equals(path1, path2)) {
                if (path1 != null) {
                    result = result.resolve(path1);
                }
                path1 = i1.next();
                path2 = i2.next();
            }
            //            int n = Math.min(p1.getNameCount(), p2.getNameCount());
            //            for (int i = 0; i < n; i++) {
            //                if (p1.getName(i)
            //                        .equals(p2.getName(i))) {
            //                    result = result.resolve(p1.getName(i));
            //                } else {
            //                    return result;
            //                }
            //            }
        }
        return result;
    }

    private static Path findWalkRoot(Path dir) {
        Path root;
        if (dir.isAbsolute()) {
            root = dir.getRoot();
        } else {
            root = Paths.get("");
        }
        for (Path path : dir) {
            if (path.toString()
                    .contains(STAR_REPLACEMENT)) {
                return root;
            }
            root = root.resolve(path);
        }
        return root;
    }

    public static String getNameSuffix(String fileName) {
        int extensionPosition = fileName.lastIndexOf('.');
        if (extensionPosition > -1) {
            int nameSuffixPosition = fileName.substring(0, extensionPosition)
                    .lastIndexOf('.');
            if (nameSuffixPosition > -1) {
                return fileName.substring(nameSuffixPosition + 1, extensionPosition);
            }
        }
        return null;
    }

    public static String getNameWithoutSuffix(String fileName) {
        int extensionPosition = fileName.lastIndexOf('.');
        if (extensionPosition > -1) {
            int nameSuffixPosition = fileName.substring(0, extensionPosition)
                    .lastIndexOf('.');
            if (nameSuffixPosition > -1) {
                return fileName.substring(0, nameSuffixPosition);
            }
            return fileName.substring(0, extensionPosition);
        }
        return fileName;
    }

    public static String computeRelativePath(Path file, String folderPath) {
        Path folder = Paths.get(folderPath)
                .normalize();
        Path relativize = folder.relativize(file);
        return normalizePath(relativize);
    }

    public static String normalizePath(Path relativize) {
        return relativize
                .toString()
                .replace("\\", "/");
    }

    static String sanitizeStringPath(String s) {
        return s.replace("*", STAR_REPLACEMENT);
    }

    static String unsanitizeStringPath(String s) {
        return s.replace(STAR_REPLACEMENT, "*");
    }

}
