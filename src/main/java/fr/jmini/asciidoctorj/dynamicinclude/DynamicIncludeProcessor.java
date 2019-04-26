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
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.asciidoctor.ast.Document;
import org.asciidoctor.extension.IncludeProcessor;
import org.asciidoctor.extension.PreprocessorReader;

import fr.jmini.utils.substringfinder.Range;
import fr.jmini.utils.substringfinder.SubstringFinder;

public class DynamicIncludeProcessor extends IncludeProcessor {
    private static final String PREFIX = "dynamic:";

    private static final Pattern TITLE_REGEX = Pattern.compile("(\\/?\\/? *)(={1,5})(.+)");

    private static final SubstringFinder DOUBLE_ANGLED_BRACKET_FINDER = SubstringFinder.define("<<", ">>");
    private static final SubstringFinder SINGLE_BRACKET_FINDER = SubstringFinder.define("[", "]");

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

        String idprefix = document.getAttribute("idprefix", "_")
                .toString();
        String idseparator = document.getAttribute("idseparator", "_")
                .toString();

        List<FileHolder> contentFiles = files.stream()
                .map(p -> createFileHolder(dir, p, idprefix, idseparator))
                .collect(Collectors.toList());

        List<FileHolder> list = sortList(contentFiles, orderList, FileHolder::getKey);

        for (int i = list.size() - 1; i >= 0; i--) {
            FileHolder item = list.get(i);
            Path path = item.getPath();
            File file = path.toFile();

            boolean previousTitleEquals = false;
            if (i > 0) {
                FileHolder previousItem = list.get(i - 1);
                previousTitleEquals = (item.getTitleType() == TitleType.PRESENT)
                        && Objects.equals(previousItem.getTitleType(), item.getTitleType())
                        && Objects.equals(previousItem.getTitleLevel(), item.getTitleLevel())
                        && Objects.equals(previousItem.getTitle(), item.getTitle());
            }

            int splitIndex = (previousTitleEquals) ? item.getTitleEnd() : item.getTitleStart();
            String header = item.getContent()
                    .substring(0, splitIndex);
            int lineNumber = countLines(header);

            String prefix;
            if (previousTitleEquals) {
                prefix = "";
            } else if (item.getTitleType() == TitleType.PRESENT) {
                prefix = "\n";
                lineNumber = lineNumber - 1;
            } else {
                prefix = "[#" + item.getTitleId() + "]\n";
                lineNumber = lineNumber - 1;
            }

            String content = prefix + item.getContent()
                    .substring(splitIndex);
            content = replaceXrefDoubleAngledBracketLinks(content, list, dir, path, root);
            content = replaceXrefInlineLinks(content, list, dir, path, root);

            reader.push_include(content, file.getName(), path.toString(), lineNumber, attributes);
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

    public static FileHolder createFileHolder(Path dir, Path p, String idprefix, String idseparator) {
        String key = dir.relativize(p)
                .toString();
        String content = readFile(p);

        TitleType titleType;
        int titleLevel;
        String title;
        String titleId;
        int titleStart;
        int titleEnd;
        Matcher titleMatcher = TITLE_REGEX.matcher(content);
        if (titleMatcher.find()) {
            titleType = titleMatcher.group(1)
                    .isEmpty() ? TitleType.PRESENT : TitleType.COMMENTED;
            titleLevel = titleMatcher.group(2)
                    .length();
            title = titleMatcher.group(3)
                    .trim();
            titleId = computeTitleId(title, idprefix, idseparator);
            titleStart = titleMatcher.start();
            titleEnd = titleMatcher.end();
        } else {
            titleType = TitleType.ABSENT;
            titleLevel = 0;
            title = null;
            titleId = computeTitleId(key, idprefix, idseparator);
            titleStart = 0;
            titleEnd = 0;
        }

        return new FileHolder(p, key, content, titleType, title, titleLevel, titleId, titleStart, titleEnd);
    }

    public static String computeTitleId(String text, String idprefix, String idseparator) {
        StringBuilder sb = new StringBuilder();
        if (idprefix != null) {
            sb.append(idprefix);
        }
        String anchor = text;
        anchor = anchor.replace("/", "");
        anchor = anchor.replaceAll("[^\\w]", " ");
        anchor = anchor.trim();
        anchor = anchor.toLowerCase();
        if (idseparator != null) {
            anchor = anchor.replaceAll("\\s+", idseparator);
        }
        sb.append(anchor);
        return sb.toString();
    }

    public static String replaceXrefDoubleAngledBracketLinks(String content, List<FileHolder> list, Path dir, Path currentPath, Path currentRoot) {
        return replaceXref(content, list, dir, currentPath, currentRoot, DynamicIncludeProcessor::findNextXrefDoubleAngledBracket);
    }

    public static String replaceXrefInlineLinks(String content, List<FileHolder> list, Path dir, Path currentPath, Path currentRoot) {
        if (list.isEmpty()) {
            return content;
        }
        StringBuilder sb = new StringBuilder();

        int startAt = 0;
        int findXref = content.indexOf("xref:", startAt);
        while (findXref > -1) {

            Optional<Range> find = SINGLE_BRACKET_FINDER.nextRange(content, findXref);
            if (find.isPresent()) {
                Range range = find.get();
                String target = content.substring(findXref + 5, range.getRangeStart());
                if (target.matches("\\S+")) {

                    sb.append(content.substring(startAt, findXref));
                    sb.append("xref:");

                    int hashPosition = target.indexOf("#");
                    String fileName = hashPosition > -1 ? target.substring(0, hashPosition) : target;

                    Path file;
                    if (fileName.startsWith("{root}")) {
                        file = currentRoot.resolve(fileName.substring(6));
                    } else {
                        file = currentPath.getParent()
                                .resolve(fileName);
                    }
                    Optional<FileHolder> findFile = findByFile(list, file);
                    if (!findFile.isPresent()) {
                        sb.append(dir.relativize(file)
                                .toString());
                    }
                    if (hashPosition > -1) {
                        String anchor = target.substring(hashPosition + 1);
                        if (anchor.trim()
                                .isEmpty() && findFile.isPresent()) {
                            sb.append("#");
                            sb.append(findFile.get()
                                    .getTitleId());
                        } else {
                            sb.append("#");
                            sb.append(anchor);
                        }
                    } else if (findFile.isPresent()) {
                        sb.append("#");
                        sb.append(findFile.get()
                                .getTitleId());
                    }

                    sb.append(content.substring(range.getRangeStart(), range.getRangeEnd()));
                    startAt = range.getRangeEnd();
                } else {
                    startAt = findXref;
                }
            } else {
                startAt = findXref;
            }

            findXref = content.indexOf("xref:", startAt);
        }
        if (startAt < content.length()) {
            sb.append(content.substring(startAt));
        }
        return sb.toString();
    }

    private static String replaceXref(String content, List<FileHolder> list, Path dir, Path currentPath, Path currentRoot, BiFunction<String, Integer, Optional<XrefHolder>> findFunction) {
        if (list.isEmpty()) {
            return content;
        }
        StringBuilder sb = new StringBuilder();

        int startAt = 0;
        Optional<XrefHolder> find = findFunction.apply(content, startAt);
        while (find.isPresent()) {
            XrefHolder holder = find.get();

            sb.append(content.substring(startAt, holder.getStartIndex()));
            XrefHolder replacedHolder = replaceHolder(holder, list, dir, currentPath, currentRoot);
            sb.append(holderToAsciiDoc(replacedHolder));

            startAt = holder.getEndIndex();
            find = findNextXrefDoubleAngledBracket(content, startAt);
        }

        if (startAt < content.length()) {
            sb.append(content.substring(startAt));
        }
        return sb.toString();
    }

    public static Optional<XrefHolder> findNextXrefDoubleAngledBracket(String content, int startAt) {
        Optional<Range> find = DOUBLE_ANGLED_BRACKET_FINDER.nextRange(content, startAt);
        if (find.isPresent()) {
            Range range = find.get();

            String fileName;
            String anchor;
            String text;
            String rangeContent = content.substring(range.getContentStart(), range.getContentEnd());
            int hashPosition = rangeContent.indexOf("#");

            int searchStart;
            if (hashPosition > -1) {
                fileName = rangeContent.substring(0, hashPosition);
                searchStart = hashPosition + 1;
            } else {
                fileName = null;
                searchStart = 0;
            }

            int commaPosition = rangeContent.indexOf(",", searchStart);
            if (commaPosition > -1) {
                anchor = rangeContent.substring(searchStart, commaPosition);
                text = rangeContent.substring(commaPosition + 1);
            } else {
                anchor = rangeContent.substring(searchStart);
                text = null;
            }

            return Optional.of(new XrefHolder(fileName, anchor, text, true, range.getRangeStart(), range.getRangeEnd()));
        }
        return Optional.empty();
    }

    private static XrefHolder replaceHolder(XrefHolder holder, List<FileHolder> list, Path dir, Path currentPath, Path currentRoot) {
        String newFileName;
        String newAnchor = null;
        String fileName = holder.getFile();
        if (fileName != null) {
            Path file;
            if (fileName.startsWith("{root}")) {
                file = currentRoot.resolve(fileName.substring(6));
            } else {
                file = currentPath.getParent()
                        .resolve(fileName);
            }
            Optional<FileHolder> findFile = findByFile(list, file);
            if (!findFile.isPresent()) {
                newFileName = dir.relativize(file)
                        .toString();
            } else {
                newFileName = "";
            }
            if (holder.getAnchor()
                    .trim()
                    .isEmpty()) {
                newAnchor = findFile.get()
                        .getTitleId();
            }
        } else {
            newFileName = null;
        }
        if (newAnchor == null) {
            newAnchor = holder.getAnchor();
        }
        return new XrefHolder(newFileName, newAnchor, holder.getText(), holder.isDoubleAngledBracketForm(), -1, -1);
    }

    public static String holderToAsciiDoc(XrefHolder holder) {
        StringBuilder sb = new StringBuilder();
        if (holder.isDoubleAngledBracketForm()) {
            sb.append("<<");
            if (holder.getFile() != null) {
                sb.append(holder.getFile());
                sb.append("#");
            }
            if (holder.getAnchor() != null) {
                sb.append(holder.getAnchor());
            }
            if (holder.getText() != null) {
                sb.append(",");
                sb.append(holder.getText());
            }
            sb.append(">>");
        }
        return sb.toString();
    }

    private static boolean isFilePresent(List<FileHolder> list, Path file) {
        Optional<FileHolder> find = findByFile(list, file);
        return find.isPresent();
    }

    private static Optional<FileHolder> findByFile(List<FileHolder> list, Path file) {
        Optional<FileHolder> find = list.stream()
                .filter(i -> Objects.equals(file.normalize(), i.getPath()))
                .findAny();
        return find;
    }

    public static int countLines(String string) {
        Matcher m = Pattern.compile("\r\n|\r|\n")
                .matcher(string);
        int counter = 1;
        while (m.find()) {
            counter++;
        }
        return counter;
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
