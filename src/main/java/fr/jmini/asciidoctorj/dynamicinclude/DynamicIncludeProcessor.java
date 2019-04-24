package fr.jmini.asciidoctorj.dynamicinclude;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
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
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.asciidoctor.ast.Document;
import org.asciidoctor.extension.IncludeProcessor;
import org.asciidoctor.extension.PreprocessorReader;

import fr.jmini.utils.substringfinder.Range;
import fr.jmini.utils.substringfinder.SubstringFinder;

public class DynamicIncludeProcessor extends IncludeProcessor {

    private static final String PREFIX = "dynamic:";
    private static final SubstringFinder DOUBLE_ANGLED_BRACKET_FINDER = SubstringFinder.define("<<", ">>");

    public DynamicIncludeProcessor() {
        super();
    }

    public DynamicIncludeProcessor(Map<String, Object> config) {
        super(config);
    }

    @Override
    public boolean handles(String target) {
        return target.startsWith(PREFIX);
    }

    @Override
    public void process(Document document, PreprocessorReader reader, String target, Map<String, Object> attributes) {
        Path dir = Paths.get(reader.getDir());
        String glob = target.substring(PREFIX.length());

        List<Path> files = findFiles(dir, glob);

        String order;
        if (attributes.containsKey("order")) {
            order = attributes.get("order")
                    .toString();
        } else if (document.hasAttribute("dynamic-include-order")) {
            order = document.getAttribute("dynamic-include-order")
                    .toString();
        } else {
            order = null;
        }
        Path root;
        if (document.hasAttribute("root")) {
            root = dir.resolve(document.getAttribute("root")
                    .toString());
        } else {
            root = dir;
        }

        List<String> orderList;
        if (order != null) {
            try {
                Path oderFile = dir.resolve(order);
                if (Files.isReadable(oderFile)) {
                    orderList = Files.readAllLines(oderFile);
                } else {
                    System.out.println("Could not find order file:" + oderFile.toAbsolutePath());
                    orderList = Collections.emptyList();
                }
            } catch (IOException e) {
                //TODO: do something else with the exception
                e.printStackTrace();
                orderList = Collections.emptyList();
            }
        } else {
            orderList = Collections.emptyList();
        }
        List<Path> list = sortList(files, orderList, p -> dir.relativize(p)
                .toString());

        for (int i = list.size() - 1; i >= 0; i--) {
            Path path = list.get(i);
            File file = path.toFile();

            String content = readFile(path);
            content = replaceXrefDoubleAngledBracketLinks(content, list, dir, path, root);
            content = replaceXrefInlineLinks(content, list);
            reader.push_include(content, file.getName(), path.toString(), 1, attributes);
        }
    }

    public static List<Path> findFiles(Path dir, String glob) {
        final PathMatcher matcher = FileSystems.getDefault()
                .getPathMatcher("glob:" + dir + File.separator + glob);

        List<Path> result = new ArrayList<>();
        try {
            Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (matcher.matches(file)) {
                        result.add(file);
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

    public static <T> List<T> sortList(List<T> list, List<String> orderedKeys, Function<T, String> keyExtractor) {
        return list.stream()
                .sorted(Comparator.comparingInt((T i) -> indexOfOrMaxValue(orderedKeys, keyExtractor.apply(i), list.size()))
                        .thenComparing(Comparator.comparing(keyExtractor)))
                .collect(Collectors.toList());
    }

    private static int indexOfOrMaxValue(List<String> orderedKeys, String value, int maxValue) {
        int index = orderedKeys.indexOf(value);
        if (index == -1) {
            return maxValue;
        }
        return index;
    }

    static String readFile(Path file) {
        String content;
        try {
            content = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
            throw new IllegalStateException("Could not read file: " + file, e);
        }
        return content;
    }

    public static String replaceXrefDoubleAngledBracketLinks(String content, List<Path> list, Path dir, Path currentPath, Path currentRoot) {
        if (list.isEmpty()) {
            return content;
        }
        StringBuilder sb = new StringBuilder();

        int startAt = 0;
        Optional<Range> find = DOUBLE_ANGLED_BRACKET_FINDER.nextRange(content);
        while (find.isPresent()) {
            Range range = find.get();

            sb.append(content.substring(startAt, range.getRangeStart()));
            sb.append("<<");
            String rangeContent = content.substring(range.getContentStart(), range.getContentEnd());
            int hashPosition = rangeContent.indexOf("#");
            if (hashPosition > -1) {
                String fileName = rangeContent.substring(0, hashPosition);

                Path file;
                if (fileName.startsWith("{root}")) {
                    file = currentRoot.resolve(fileName.substring(6));
                } else {
                    file = currentPath.getParent()
                            .resolve(fileName);
                }
                if (!list.contains(file)) {
                    sb.append(dir.relativize(file)
                            .toString());
                }
                sb.append(rangeContent.substring(hashPosition));
            } else {
                sb.append(rangeContent);
            }
            sb.append(">>");

            startAt = range.getRangeEnd();
            find = DOUBLE_ANGLED_BRACKET_FINDER.nextRange(content, startAt);
        }
        if (startAt < content.length()) {
            sb.append(content.substring(startAt));
        }
        return sb.toString();
    }

    public static String replaceXrefInlineLinks(String content, List<Path> list) {
        if (list.isEmpty()) {
            return content;
        }
        StringBuilder sb = new StringBuilder();
        int startAt = 0;

        if (startAt < content.length()) {
            sb.append(content.substring(startAt));
        }
        return sb.toString();
    }
}
