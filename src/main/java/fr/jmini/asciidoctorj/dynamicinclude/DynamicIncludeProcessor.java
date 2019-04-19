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
import java.util.function.Function;
import java.util.stream.Collectors;

import org.asciidoctor.ast.Document;
import org.asciidoctor.extension.IncludeProcessor;
import org.asciidoctor.extension.PreprocessorReader;

public class DynamicIncludeProcessor extends IncludeProcessor {

    private static final String PREFIX = "dynamic:";

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
        } else if (document.getAttributes()
                .containsKey("dynamic-include-order")) {
            order = document.getAttributes()
                    .get("dynamic-include-order")
                    .toString();
        } else {
            order = null;
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
}
