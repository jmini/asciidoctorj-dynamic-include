package fr.jmini.asciidoctorj.dynamicinclude;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.asciidoctor.ast.Document;
import org.asciidoctor.extension.IncludeProcessor;
import org.asciidoctor.extension.PreprocessorReader;
import org.asciidoctor.log.LogRecord;
import org.asciidoctor.log.Severity;

import fr.jmini.asciidoctorj.dynamicinclude.XrefHolder.XrefHolderType;
import fr.jmini.asciidoctorj.dynamicinclude.path.PathUtil;
import fr.jmini.utils.substringfinder.Range;
import fr.jmini.utils.substringfinder.SubstringFinder;

public class DynamicIncludeProcessor extends IncludeProcessor {
    private static final String PREFIX = "dynamic:";

    private static final Pattern TITLE_REGEX = Pattern.compile("(\\/?\\/? *)(={1,5})(.+)");

    private static final SubstringFinder DOUBLE_ANGLED_BRACKET_FINDER = SubstringFinder.define("<<", ">>");
    private static final SubstringFinder SINGLE_BRACKET_FINDER = SubstringFinder.define("[", "]");
    private static final SubstringFinder SINGLE_CURLY_BRACKET_FINDER = SubstringFinder.define("{", "}");

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
        Consumer<String> logger = (String message) -> log(new LogRecord(Severity.WARN, message));
        Path dir = Paths.get(reader.getDir());
        String glob = target.substring(PREFIX.length());

        String order = readKey(document, attributes, "order", "dynamic-include-order");
        boolean externalXrefAsText = hasKey(document, attributes, "external-xref-as-text", "dynamic-include-external-xref-as-text");

        String scopesValue = readKey(document, attributes, "scopes", "dynamic-include-scopes");
        String scopesOrderValue = readKey(document, attributes, "scopes-order", "dynamic-include-scopes-order");
        String areasValue = readKey(document, attributes, "areas", "dynamic-include-areas");
        String areasOrderValue = readKey(document, attributes, "areas-order", "dynamic-include-areas-order");

        String logfile = readKey(document, attributes, "logfile", "dynamic-include-logfile");

        boolean displayViewSourceLink = hasKey(document, attributes, "display-view-source", "dynamic-include-display-view-source");
        String viewSourceLinkPattern = readKey(document, attributes, "view-source-link-pattern", "dynamic-include-view-source-link-pattern");
        String viewSourceLinkText = readKey(document, attributes, "view-source-link-text", "dynamic-include-view-source-link-text");
        if (viewSourceLinkText == null) {
            viewSourceLinkText = "view source";
        }

        Path root;
        if (document.hasAttribute("root")) {
            root = dir.resolve(document.getAttribute("root")
                    .toString())
                    .normalize();
        } else {
            root = dir;
        }
        List<Path> files = PathUtil.findFiles(dir, glob, Collections.emptyList());
        List<Path> sortedFiles = PathUtil.sortFiles(logger, files);

        String idprefix = document.getAttribute("idprefix", "_")
                .toString();
        String idseparator = document.getAttribute("idseparator", "_")
                .toString();
        List<FileHolder> list = sortedFiles.stream()
                .map(p -> createFileHolder(dir, root, p, idprefix, idseparator))
                .collect(Collectors.toList());

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
                    Files.createDirectories(path.getParent());
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

            StringBuilder prefixSb = new StringBuilder();

            if (displayViewSourceLink) {
                String viewSourceUrl = replacePlaceholders(viewSourceLinkPattern, path, (String k) -> getDocumentAttribute(document, k));

                lineNumber = lineNumber - 3;
                prefixSb.append("\n");
                prefixSb.append("[.dynamic-include-view-source]\n");
                prefixSb.append("[ link:" + viewSourceUrl + "[" + viewSourceLinkText + "] ]\n");
            }

            if (!previousTitleEquals) {
                if (item.getTitleType() == TitleType.PRESENT) {
                    prefixSb.append("\n");
                    lineNumber = lineNumber - 1;
                } else {
                    prefixSb.append("[#" + item.getTitleId() + "]\n");
                    lineNumber = lineNumber - 1;
                }
            }

            String content = prefixSb.toString() + item.getContent()
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
            Optional<String> documentAttribute = getDocumentAttribute(document, documentKey);
            if (documentAttribute.isPresent()) {
                return documentAttribute.get();
            }
        }
        return null;
    }

    private Optional<String> getDocumentAttribute(Document document, String key) {
        if (document.hasAttribute(key)) {
            return Optional.ofNullable(document.getAttribute(key)
                    .toString());
        }
        return Optional.empty();
    }

    private boolean hasKey(Document document, Map<String, Object> attributes, String includeKey, String documentKey) {
        if (attributes.containsKey(includeKey)) {
            return true;
        }
        return document.hasAttribute(documentKey);
    }

    public static FileHolder createFileHolder(Path dir, Path root, Path p, String idprefix, String idseparator) {
        String key = dir.relativize(p)
                .toString()
                .replace('\\', '/');

        String fileName = p.toFile()
                .getName();

        String nameSuffix = PathUtil.getNameSuffix(fileName);

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

        return new FileHolder(p, key, nameSuffix, content, titleType, title, titleLevel, titleId, titleStart, titleEnd);
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

    public static String replacePlaceholders(String viewSourceLinkPattern, Path file, Function<String, Optional<String>> attributeGetter) {
        StringBuilder sb = new StringBuilder();

        int startAt = 0;
        while (startAt < viewSourceLinkPattern.length()) {
            Optional<Range> find = SINGLE_CURLY_BRACKET_FINDER.nextRange(viewSourceLinkPattern, startAt);
            if (find.isPresent()) {
                Range range = find.get();
                sb.append(viewSourceLinkPattern.substring(startAt, range.getRangeStart()));
                String placeholderName = viewSourceLinkPattern.substring(range.getContentStart(), range.getContentEnd());
                String placeholderLowerCase = placeholderName.toLowerCase();
                switch (placeholderLowerCase) {
                case "file-relative-to-git-repository":
                    Optional<String> localGitRepositoryPath = attributeGetter.apply("local-git-repository-path");
                    appendPlaceholderValue(sb, placeholderName, localGitRepositoryPath, (folderPath) -> PathUtil.computeRelativePath(file, folderPath));
                    break;
                case "file-relative-to-gradle-projectdir":
                    Optional<String> gradleProjectdir = attributeGetter.apply("gradle-projectdir");
                    appendPlaceholderValue(sb, placeholderName, gradleProjectdir, (folderPath) -> PathUtil.computeRelativePath(file, folderPath));
                    break;
                case "file-relative-to-gradle-rootdir":
                    Optional<String> gradleRootdir = attributeGetter.apply("gradle-rootdir");
                    appendPlaceholderValue(sb, placeholderName, gradleRootdir, (folderPath) -> PathUtil.computeRelativePath(file, folderPath));
                    break;
                case "file-absolute-with-leading-slash":
                    String absolutePath = PathUtil.normalizePath(file.toAbsolutePath());
                    if (!absolutePath.startsWith("/")) {
                        sb.append("/");
                    }
                    sb.append(absolutePath);
                    break;
                default:
                    Optional<String> attribute = attributeGetter.apply(placeholderLowerCase);
                    appendPlaceholderValue(sb, placeholderName, attribute, Function.identity());
                    break;
                }
                startAt = range.getRangeEnd();
            } else {
                sb.append(viewSourceLinkPattern.substring(startAt));
                startAt = viewSourceLinkPattern.length();
            }
        }
        return sb.toString();
    }

    private static void appendPlaceholderValue(StringBuilder sb, String placeholderName, Optional<String> attribute, Function<String, String> converter) {
        if (attribute.isPresent()) {
            sb.append(converter.apply(attribute.get()));
        } else {
            sb.append("{");
            sb.append(placeholderName);
            sb.append("}");
        }
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
