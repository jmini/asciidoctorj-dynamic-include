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
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
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

import fr.jmini.asciidoctorj.dynamicinclude.XrefHolder.XrefHolderType;
import fr.jmini.utils.substringfinder.Range;
import fr.jmini.utils.substringfinder.SubstringFinder;

public class DynamicIncludeProcessor extends IncludeProcessor {
    private static final String PREFIX = "dynamic:";

    private static final String STAR_REPLACEMENT = "__STAR__";

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

        String order = readKey(document, attributes, "order", "dynamic-include-order");
        boolean externalXrefAsText = readKey(document, attributes, "external-xref-as-text", "dynamic-include-external-xref-as-text") != null;

        String scopesValue = readKey(document, attributes, "scopes", "dynamic-include-scopes");
        String scopesOrderValue = readKey(document, attributes, "scopes-order", "dynamic-include-scopes-order");
        String areasValue = readKey(document, attributes, "areas", "dynamic-include-areas");
        String areasOrderValue = readKey(document, attributes, "areas-order", "dynamic-include-areas-order");

        String logfile = readKey(document, attributes, "logfile", "dynamic-include-logfile");

        List<String> scopes = valueToList(scopesValue);
        List<String> scopesOrder = scopesOrderValue != null ? valueToList(scopesOrderValue) : scopes;
        List<String> areas = valueToList(areasValue);
        List<String> areasOrder = areasOrderValue != null ? valueToList(areasOrderValue) : areas;

        Path root;
        if (document.hasAttribute("root")) {
            root = dir.resolve(document.getAttribute("root")
                    .toString())
                    .normalize();
        } else {
            root = dir;
        }
        List<Path> files = findFiles(dir, root, glob, scopes, areas);

        List<String> patternOrder;
        if (order != null) {
            try {
                Path orderFile = dir.resolve(order);
                Path orderFileFolder = orderFile.getParent();
                if (Files.isReadable(orderFile)) {
                    patternOrder = Files.readAllLines(orderFile)
                            .stream()
                            .map(String::trim)
                            .filter(s -> !s.isEmpty())
                            .filter(s -> !s.startsWith("//"))
                            .filter(s -> !s.startsWith("#"))
                            .map(s -> orderFileFolder.resolve(sanitizeStringPath(s)))
                            .map(p -> unsanitizeStringPath(dir.relativize(p)
                                    .toString()
                                    .replace('\\', '/')))
                            .map(DynamicIncludeProcessor::convertGlobToRegex)
                            .collect(Collectors.toList());
                } else {
                    System.out.println("Could not find order file:" + orderFile.toAbsolutePath());
                    patternOrder = Collections.emptyList();
                }
            } catch (IOException e) {
                //TODO: do something else with the exception
                e.printStackTrace();
                patternOrder = Collections.emptyList();
            }
        } else {
            patternOrder = Collections.emptyList();
        }

        String idprefix = document.getAttribute("idprefix", "_")
                .toString();
        String idseparator = document.getAttribute("idseparator", "_")
                .toString();

        List<FileHolder> contentFiles = files.stream()
                .map(p -> createFileHolder(dir, root, p, idprefix, idseparator))
                .collect(Collectors.toList());

        List<FileHolder> list = sortList(contentFiles, patternOrder, scopesOrder, areasOrder);
        if (logfile != null) {
            StringBuilder sb = new StringBuilder();

            sb.append("# File: ");
            sb.append(reader.getFile());
            sb.append("\n");

            sb.append("# Target: ");
            sb.append(target);
            sb.append("\n");

            list.forEach(h -> sb.append(h.getKey())
                    .append("\n"));

            Path path = Paths.get(logfile);
            try {
                if (Files.notExists(path)) {
                    Files.createFile(path);
                }
                Files.write(path, sb.toString()
                        .getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

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
            content = replaceXrefDoubleAngledBracketLinks(content, list, dir, path, root, externalXrefAsText);
            content = replaceXrefInlineLinks(content, list, dir, path, root, externalXrefAsText);

            reader.push_include(content, file.getName(), path.toString(), lineNumber, attributes);
        }
    }

    private String readKey(Document document, Map<String, Object> attributes, String includeKey, String documentKey) {
        if (attributes.containsKey(includeKey)) {
            return attributes.get(includeKey)
                    .toString();
        } else {
            if (document.hasAttribute(documentKey)) {
                return document.getAttribute(documentKey)
                        .toString();
            }
        }
        return null;
    }

    public static List<Path> findFiles(Path dir, Path root, String glob, List<String> scopes, List<String> areas) {
        Path normalizedGlob = dir.resolve(sanitizeStringPath(glob))
                .normalize();
        final PathMatcher matcher = FileSystems.getDefault()
                .getPathMatcher("glob:" + unsanitizeStringPath(normalizedGlob.toString()
                        .replace('\\', '/')));

        List<Path> result = new ArrayList<>();
        try {
            Path walkRoot = findWalkRoot(normalizedGlob);
            Files.walkFileTree(walkRoot, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (matcher.matches(file)) {
                        if (!scopes.isEmpty() || !areas.isEmpty()) {
                            Path path = root.relativize(file);
                            Iterator<Path> iterator = path.iterator();
                            if (!scopes.isEmpty() && iterator.hasNext()) {
                                String scope = iterator.next()
                                        .toString();
                                if (scopes.contains(scope)) {
                                    if (areas.isEmpty()) {
                                        result.add(file);
                                    } else if (!scopes.isEmpty() && iterator.hasNext()) {
                                        String area = iterator.next()
                                                .toString();
                                        if (areas.contains(area)) {
                                            result.add(file);
                                        }
                                    }
                                }
                            }
                        } else {
                            result.add(file);
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

    private static List<String> valueToList(String topicsValue) {
        if (topicsValue != null) {
            return Arrays.asList(topicsValue.split(":"));
        }
        return Collections.emptyList();
    }

    /**
     * Converts a standard POSIX Shell globbing pattern into a regular expression pattern. The result can be used with the standard {@link java.util.regex} API to recognize strings which match the glob pattern.
     * <p>
     * See also, the POSIX Shell language: http://pubs.opengroup.org/onlinepubs/009695399/utilities/xcu_chap02.html#tag_02_13_01
     *
     * @param pattern
     *            A glob pattern.
     * @return A regex pattern to recognize the given glob pattern.
     */
    public static final String convertGlobToRegex(String pattern) {
        StringBuilder sb = new StringBuilder(pattern.length());
        int inGroup = 0;
        int inClass = 0;
        int firstIndexInClass = -1;
        char[] arr = pattern.toCharArray();
        for (int i = 0; i < arr.length; i++) {
            char ch = arr[i];
            switch (ch) {
            case '\\':
                if (++i >= arr.length) {
                    sb.append('\\');
                } else {
                    char next = arr[i];
                    switch (next) {
                    case ',':
                        // escape not needed
                        break;
                    case 'Q':
                    case 'E':
                        // extra escape needed
                        sb.append('\\');
                    default:
                        sb.append('\\');
                    }
                    sb.append(next);
                }
                break;
            case '*':
                if (inClass == 0)
                    sb.append(".*");
                else
                    sb.append('*');
                break;
            case '?':
                if (inClass == 0)
                    sb.append('.');
                else
                    sb.append('?');
                break;
            case '[':
                inClass++;
                firstIndexInClass = i + 1;
                sb.append('[');
                break;
            case ']':
                inClass--;
                sb.append(']');
                break;
            case '.':
            case '(':
            case ')':
            case '+':
            case '|':
            case '^':
            case '$':
            case '@':
            case '%':
                if (inClass == 0 || (firstIndexInClass == i && ch == '^'))
                    sb.append('\\');
                sb.append(ch);
                break;
            case '!':
                if (firstIndexInClass == i)
                    sb.append('^');
                else
                    sb.append('!');
                break;
            case '{':
                inGroup++;
                sb.append('(');
                break;
            case '}':
                inGroup--;
                sb.append(')');
                break;
            case ',':
                if (inGroup > 0)
                    sb.append('|');
                else
                    sb.append(',');
                break;
            default:
                sb.append(ch);
            }
        }
        return sb.toString();
    }

    public static List<FileHolder> sortList(List<FileHolder> list, List<String> patternOrder, List<String> scopesOrder, List<String> areasOrder) {
        Comparator<FileHolder> comparator = getOrderedKeyPatternComparator(list, patternOrder, FileHolder::getKey)
                .thenComparing(getOrderedValuesComparator(list, scopesOrder, FileHolder::getPathScope, FileHolder::getKey))
                .thenComparing(getOrderedValuesComparator(list, areasOrder, FileHolder::getPathArea, FileHolder::getKey))
                .thenComparing(Comparator.comparing(FileHolder::getKey));

        return list.stream()
                .sorted(comparator)
                .collect(Collectors.toList());
    }

    public static <T> Comparator<T> getOrderedKeyPatternComparator(List<T> list, List<String> orderedKeyPatterns, Function<T, String> keyExtractor) {
        Comparator<T> comparator;
        if (orderedKeyPatterns.isEmpty()) {
            comparator = (t1, t2) -> 0;
        } else {
            final Map<String, Integer> orderMap = list.stream()
                    .collect(Collectors.toMap(keyExtractor, v -> orderOrMaxValue(orderedKeyPatterns, keyExtractor.apply(v), list.size())));
            comparator = Comparator.comparingInt((final T i) -> orderMap.get(keyExtractor.apply(i)));
        }
        return comparator;
    }

    private static int orderOrMaxValue(List<String> orderedKeyPatterns, String value, int maxValue) {
        for (int i = 0; i < orderedKeyPatterns.size(); i++) {
            String p = orderedKeyPatterns.get(i);
            if (value.matches(p)) {
                return i;
            }
        }

        System.out.println("Did not find any information order for '" + value + "', putting it at the end of the document");
        return maxValue;
    }

    public static <T> Comparator<T> getOrderedValuesComparator(List<T> list, List<String> orderedValues, Function<T, String> valueExtractor, Function<T, String> identifierExtractor) {
        Comparator<T> comparator;
        if (orderedValues.isEmpty()) {
            comparator = (t1, t2) -> 0;
        } else {
            final Map<String, Integer> orderMap = list.stream()
                    .collect(Collectors.toMap(identifierExtractor, v -> indexOfOrMaxValue(orderedValues, valueExtractor.apply(v), list.size())));
            comparator = Comparator.comparingInt((final T i) -> orderMap.get(identifierExtractor.apply(i)));
        }
        return comparator;
    }

    private static int indexOfOrMaxValue(List<String> orderedValues, String value, int maxValue) {
        int indexOf = orderedValues.indexOf(value);
        return (indexOf > -1) ? indexOf : maxValue;
    }

    public static FileHolder createFileHolder(Path dir, Path root, Path p, String idprefix, String idseparator) {
        String key = dir.relativize(p)
                .toString()
                .replace('\\', '/');
        Iterator<Path> iterator = root.relativize(p)
                .iterator();
        String scope;
        String area;
        if (iterator.hasNext()) {
            scope = iterator.next()
                    .toString();
            if (iterator.hasNext()) {
                area = iterator.next()
                        .toString();
            } else {
                area = null;
            }
        } else {
            scope = null;
            area = null;
        }

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

        return new FileHolder(p, key, scope, area, content, titleType, title, titleLevel, titleId, titleStart, titleEnd);
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

    public static String replaceXrefDoubleAngledBracketLinks(String content, List<FileHolder> list, Path dir, Path currentPath, Path currentRoot, boolean externalXrefAsText) {
        return replaceXref(content, list, dir, currentPath, currentRoot, externalXrefAsText, DynamicIncludeProcessor::findNextXrefDoubleAngledBracket);
    }

    public static String replaceXrefInlineLinks(String content, List<FileHolder> list, Path dir, Path currentPath, Path currentRoot, boolean externalXrefAsText) {
        return replaceXref(content, list, dir, currentPath, currentRoot, externalXrefAsText, DynamicIncludeProcessor::findNextXrefInline);
    }

    private static String replaceXref(String content, List<FileHolder> list, Path dir, Path currentPath, Path currentRoot, boolean externalXrefAsText, BiFunction<String, Integer, Optional<XrefHolder>> findFunction) {
        if (list.isEmpty()) {
            return content;
        }
        StringBuilder sb = new StringBuilder();

        int startAt = 0;
        Optional<XrefHolder> find = findFunction.apply(content, startAt);
        while (find.isPresent()) {
            XrefHolder holder = find.get();

            sb.append(content.substring(startAt, holder.getStartIndex()));
            XrefHolder replacedHolder = replaceHolder(holder, list, dir, currentPath, currentRoot, externalXrefAsText);
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

            return Optional.of(new XrefHolder(fileName, anchor, text, XrefHolderType.DOUBLE_ANGLED_BRACKET, range.getRangeStart(), range.getRangeEnd()));
        }
        return Optional.empty();
    }

    public static Optional<XrefHolder> findNextXrefInline(String content, int startAt) {
        int findXref = content.indexOf("xref:", startAt);
        if (findXref > -1) {
            Optional<Range> find = SINGLE_BRACKET_FINDER.nextRange(content, findXref);
            if (find.isPresent()) {
                Range range = find.get();
                String target = content.substring(findXref + 5, range.getRangeStart());
                if (target.matches("\\S+")) {
                    String fileName;
                    String anchor;
                    int hashPosition = target.indexOf("#");
                    if (hashPosition > -1) {
                        fileName = target.substring(0, hashPosition);
                        anchor = target.substring(hashPosition + 1);
                    } else {
                        fileName = null;
                        anchor = target;
                    }
                    String text;
                    if (range.getContentStart() != range.getContentEnd()) {
                        text = content.substring(range.getContentStart(), range.getContentEnd());
                    } else {
                        text = null;
                    }
                    return Optional.of(new XrefHolder(fileName, anchor, text, XrefHolderType.INLINE, findXref, range.getRangeEnd()));
                }
            }
        }
        return Optional.empty();
    }

    private static XrefHolder replaceHolder(XrefHolder holder, List<FileHolder> list, Path dir, Path currentPath, Path currentRoot, boolean externalXrefAsText) {
        String newFileName;
        String newAnchor = null;
        String fileName = holder.getFile();
        XrefHolderType type = holder.getType();
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
                if (externalXrefAsText) {
                    type = XrefHolderType.TEXT;
                }
                newFileName = dir.relativize(file)
                        .toString()
                        .replace('\\', '/');
            } else {
                newFileName = "";
            }
            if (findFile.isPresent() && holder.getAnchor()
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
        return new XrefHolder(newFileName, newAnchor, holder.getText(), type, -1, -1);
    }

    public static String holderToAsciiDoc(XrefHolder holder) {
        StringBuilder sb = new StringBuilder();
        switch (holder.getType()) {
        case DOUBLE_ANGLED_BRACKET: {
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
            break;
        }
        case INLINE: {
            sb.append("xref:");
            if (holder.getFile() != null) {
                sb.append(holder.getFile());
                sb.append("#");
            }
            if (holder.getAnchor() != null) {
                sb.append(holder.getAnchor());
            }
            sb.append("[");
            if (holder.getText() != null) {
                sb.append(holder.getText());
            }
            sb.append("]");
            break;
        }
        case TEXT: {
            if (holder.getText() != null) {
                sb.append(holder.getText());
            }
            break;
        }
        default:
            throw new IllegalStateException("Unexpected type: " + holder.getType());
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

    static String sanitizeStringPath(String s) {
        return s.replace("*", STAR_REPLACEMENT);
    }

    static String unsanitizeStringPath(String s) {
        return s.replace(STAR_REPLACEMENT, "*");
    }
}
