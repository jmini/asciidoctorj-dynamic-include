package fr.jmini.asciidoctorj.dynamicinclude;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
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

    private static final Pattern TITLE_REGEX = Pattern.compile("^(\\/?\\/? *)(={1,5})(.+)", Pattern.MULTILINE);

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
        Path currentFile = dir.resolve(reader.getFile())
                .toAbsolutePath();
        String glob = target.substring(PREFIX.length());

        String suffixesText = readKey(document, attributes, "suffixes", "dynamic-include-suffixes");
        List<String> suffixes = valueToList(suffixesText);

        boolean externalXrefAsText = hasKey(document, attributes, "external-xref-as-text", "dynamic-include-external-xref-as-text");

        String logfile = readKey(document, attributes, "logfile", "dynamic-include-logfile");

        String levelOffsetShiftingText = readKey(document, attributes, "level-offset-shifting", "dynamic-include-level-offset-shifting");
        int levelOffsetShifting = convertLevelOffsetShifting(logger, levelOffsetShiftingText);

        boolean displayViewSourceLink = hasKey(document, attributes, "display-view-source", "dynamic-include-display-view-source");
        String viewSourceLinkPattern = readKey(document, attributes, "view-source-link-pattern", "dynamic-include-view-source-link-pattern", "#");
        String viewSourceLinkText = readKey(document, attributes, "view-source-link-text", "dynamic-include-view-source-link-text", "view source");

        Function<String, Optional<String>> attributeResolver = (String key) -> getDocumentAttribute(document, key);

        Path root;
        if (document.hasAttribute("root")) {
            root = dir.resolve(document.getAttribute("root")
                    .toString())
                    .normalize();
        } else {
            root = dir;
        }
        List<Path> files = PathUtil.findFiles(dir, glob, suffixes);
        List<Path> filteredFile = PathUtil.filterCurrentFile(files, currentFile);
        List<Path> sortedFiles = PathUtil.sortFiles(logger, filteredFile, suffixes);

        String idprefix = document.getAttribute("idprefix", "_")
                .toString();
        String idseparator = document.getAttribute("idseparator", "_")
                .toString();
        List<String> globalExistingAnchors = new ArrayList<>();
        List<FileHolder> list = sortedFiles.stream()
                .map(p -> createFileHolder(dir, p, idprefix, idseparator, levelOffsetShifting, globalExistingAnchors))
                .collect(Collectors.toList());

        if (logfile != null) {
            StringBuilder sb = new StringBuilder();

            sb.append("# File: ");
            sb.append(reader.getFile());
            sb.append("\n");

            sb.append("# Target: ");
            sb.append(target);
            sb.append("\n");

            sb.append("# level-offset-shifting: ");
            sb.append(levelOffsetShifting);
            sb.append("\n");

            list.forEach(h -> sb.append(h.getKey())
                    .append(" (leveloffset: ")
                    .append(outputOffset(h.getLevelOffset()))
                    .append(")")
                    .append("\n"));
            sb.append("\n");

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
            TitleHolder title = item.getFirstTitle();
            Path path = item.getPath();
            File file = path.toFile();

            boolean previousTitleEquals = false;
            if (i > 0) {
                FileHolder previousItem = list.get(i - 1);
                TitleHolder previousTitle = previousItem.getFirstTitle();
                previousTitleEquals = (title.getTitleType() == TitleType.PRESENT)
                        && Objects.equals(previousTitle.getTitleType(), title.getTitleType())
                        && Objects.equals(previousTitle.getTitleLevel(), title.getTitleLevel())
                        && Objects.equals(previousTitle.getTitle(), title.getTitle());
            }

            int splitIndex = (previousTitleEquals) ? title.getTitleEnd() : title.getTitleStart();
            String header = item.getContent()
                    .substring(0, splitIndex);
            int lineNumber = countLines(header);

            StringBuilder sb = new StringBuilder();

            // Add a comment line at the top of the included document, to stay compatible with all asciidoctor versions.
            // Workaround explained here: https://github.com/asciidoctor/asciidoctor/issues/3875
            sb.append("//content of " + path + "\n");
            sb.append("\n");
            lineNumber = lineNumber - 2;

            if (displayViewSourceLink) {
                String viewSourceUrl = resolveAttributesInViewSourceLinkPattern(viewSourceLinkPattern, path, attributeResolver);

                lineNumber = lineNumber - 3;
                sb.append("\n");
                sb.append("[.dynamic-include-view-source]\n");
                sb.append("[ link:" + viewSourceUrl + "[" + viewSourceLinkText + "] ]\n");
            }

            if (item.getLevelOffset() != 0) {
                sb.append("\n");
                sb.append(":leveloffset: " + outputOffset(item.getLevelOffset()) + "\n");
                sb.append("\n");
                lineNumber = lineNumber - 3;
            }
            if (!previousTitleEquals) {
                if (title.getTitleType() == TitleType.PRESENT) {
                    sb.append("\n");
                    lineNumber = lineNumber - 1;
                } else {
                    sb.append("[#" + title.getTitleId() + "]\n");
                    lineNumber = lineNumber - 1;
                }
            }

            sb.append(item.getContent()
                    .substring(splitIndex));
            if (item.getLevelOffset() != 0) {
                sb.append("\n");
                sb.append("\n");
                sb.append(":leveloffset: " + outputOffset(-1 * item.getLevelOffset()) + "\n");
            }

            String content = sb.toString();
            content = replaceXrefDoubleAngledBracketLinks(content, list, dir, item, root, externalXrefAsText, attributeResolver);
            content = replaceXrefInlineLinks(content, list, dir, item, root, externalXrefAsText, attributeResolver);

            reader.push_include(content, file.getName(), path.toString(), lineNumber, attributes);
        }
    }

    static int convertLevelOffsetShifting(Consumer<String> logger, String levelOffsetShiftingText) {
        int levelOffsetShifting;
        if (levelOffsetShiftingText != null) {
            if (levelOffsetShiftingText.matches("\\-?\\+?[0-9]+")) {
                levelOffsetShifting = Integer.parseInt(levelOffsetShiftingText);
            } else {
                logger.accept("level-offset-shifting value '" + levelOffsetShiftingText + "' is not a valid number, using 1 as fallback");
                levelOffsetShifting = 1;
            }
        } else {
            levelOffsetShifting = 1;
        }
        return levelOffsetShifting;
    }

    static String outputOffset(int offset) {
        if (offset > 0) {
            return "+" + offset;
        }
        return "" + offset;
    }

    private String readKey(Document document, Map<String, Object> attributes, String includeKey, String documentKey) {
        return readKey(document, attributes, includeKey, documentKey, null);
    }

    private String readKey(Document document, Map<String, Object> attributes, String includeKey, String documentKey, String defaultValue) {
        if (attributes.containsKey(includeKey)) {
            return attributes.get(includeKey)
                    .toString();
        } else {
            Optional<String> documentAttribute = getDocumentAttribute(document, documentKey);
            if (documentAttribute.isPresent()) {
                return documentAttribute.get();
            }
        }
        return defaultValue;
    }

    private static List<String> valueToList(String string) {
        if (string != null) {
            return Arrays.asList(string.split(":"));
        }
        return Collections.emptyList();
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

    public static FileHolder createFileHolder(Path dir, Path path, String idprefix, String idseparator, int levelOffsetShifting, List<String> globalExistingAnchors) {
        String key = dir.relativize(path)
                .toString()
                .replace('\\', '/');

        String fileName = path.toFile()
                .getName();
        String nameWithoutSuffix = PathUtil.getNameWithoutSuffix(fileName);
        String nameSuffix = PathUtil.getNameSuffix(fileName);

        String content = readFile(path);

        TitleHolder firstTitle = null;
        List<String> localExistingAnchors = new ArrayList<>();
        Map<String, String> titleAnchorMap = new HashMap<>();
        Matcher titleMatcher = TITLE_REGEX.matcher(content);
        while (titleMatcher.find()) {
            TitleHolder titleHolder = toTitleHolder(idprefix, idseparator, localExistingAnchors, titleMatcher);
            if (firstTitle == null) {
                firstTitle = titleHolder;
            }
            if (titleHolder.getTitleType() == TitleType.PRESENT) {
                localExistingAnchors.add(titleHolder.getTitleId());
                if (!titleAnchorMap.containsKey(titleHolder.getTitle())) {
                    titleAnchorMap.put(titleHolder.getTitle(), titleHolder.getTitleId());
                }
            }
        }

        if (firstTitle == null) {
            firstTitle = new TitleHolder(TitleType.ABSENT, 0, null, computeTitleId(key, idprefix, idseparator, Collections.emptyList()), 0, 0);
        }

        int offset = calculateOffset(dir, path, nameWithoutSuffix, firstTitle.getTitleLevel(), levelOffsetShifting);

        Map<String, String> anchorShift = new HashMap<>();
        for (String anchor : localExistingAnchors) {
            if (!globalExistingAnchors.contains(anchor)) {
                globalExistingAnchors.add(anchor);
            } else {
                String anchorWithoutSuffix = stripAnchorSuffix(anchor, idseparator);
                String shiftedAnchor = shiftAnchor("(shift to global anchor '" + anchor + "')", idseparator, globalExistingAnchors, anchorWithoutSuffix);
                anchorShift.put(anchor, shiftedAnchor);
                globalExistingAnchors.add(shiftedAnchor);
            }
        }

        return new FileHolder(path, key, nameWithoutSuffix, nameSuffix, content, firstTitle, offset, titleAnchorMap, anchorShift);
    }

    static String stripAnchorSuffix(String anchor, String idseparator) {
        Pattern pattern = Pattern.compile("(.+)" + idseparator + "[0-9]+");
        Matcher matcher = pattern.matcher(anchor);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return anchor;
    }

    private static TitleHolder toTitleHolder(String idprefix, String idseparator, List<String> localExistingAnchors, Matcher titleMatcher) {
        TitleType titleType = titleMatcher.group(1)
                .isEmpty() ? TitleType.PRESENT : TitleType.COMMENTED;
        int titleLevel = titleMatcher.group(2)
                .length();
        String title = titleMatcher.group(3)
                .trim();
        String titleId = computeTitleId(title, idprefix, idseparator, localExistingAnchors);
        int titleStart = titleMatcher.start();
        int titleEnd = titleMatcher.end();
        return new TitleHolder(titleType, titleLevel, title, titleId, titleStart, titleEnd);
    }

    public static String computeTitleId(String text, String idprefix, String idseparator, List<String> localExistingAnchors) {
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
        String candidate = sb.toString();
        if (!localExistingAnchors.contains(candidate)) {
            return candidate;
        }
        return shiftAnchor("(computeTitleId for '" + text + "')", idseparator, localExistingAnchors, candidate);
    }

    private static String shiftAnchor(String helpContext, String idseparator, List<String> existingAnchors, String anchorWithoutSuffix) {
        for (int i = 2; i < 1000; i++) {
            String newCandidate = anchorWithoutSuffix + idseparator + i;
            if (!existingAnchors.contains(newCandidate)) {
                return newCandidate;
            }
        }
        throw new IllegalStateException("Could not compute the anchor " + helpContext);
    }

    static int calculateOffset(Path dir, Path path, String nameWithoutSuffix, int titleLevel, int levelOffsetShifting) {
        int headerLevel = dir.relativize(path)
                .getNameCount() + levelOffsetShifting;
        if ("index".equals(nameWithoutSuffix)) {
            headerLevel = headerLevel - 1;
        }
        return headerLevel - titleLevel;
    }

    public static String replaceXrefDoubleAngledBracketLinks(String content, List<FileHolder> list, Path dir, FileHolder currentPath, Path currentRoot, boolean externalXrefAsText, Function<String, Optional<String>> attributeResolver) {
        return replaceXref(content, list, dir, currentPath, currentRoot, externalXrefAsText, DynamicIncludeProcessor::findNextXrefDoubleAngledBracket, attributeResolver);
    }

    public static String replaceXrefInlineLinks(String content, List<FileHolder> list, Path dir, FileHolder currentPath, Path currentRoot, boolean externalXrefAsText, Function<String, Optional<String>> attributeResolver) {
        return replaceXref(content, list, dir, currentPath, currentRoot, externalXrefAsText, DynamicIncludeProcessor::findNextXrefInline, attributeResolver);
    }

    private static String replaceXref(String content, List<FileHolder> list, Path dir, FileHolder currentPath, Path currentRoot, boolean externalXrefAsText, BiFunction<String, Integer, Optional<XrefHolder>> findFunction,
            Function<String, Optional<String>> attributeResolver) {
        if (list.isEmpty()) {
            return content;
        }
        StringBuilder sb = new StringBuilder();

        int startAt = 0;
        Optional<XrefHolder> find = findFunction.apply(content, startAt);
        while (find.isPresent()) {
            XrefHolder holder = find.get();

            sb.append(content.substring(startAt, holder.getStartIndex()));
            XrefHolder replacedHolder = replaceHolder(holder, list, dir, currentPath, currentRoot, externalXrefAsText, attributeResolver);
            sb.append(holderToAsciiDoc(replacedHolder));

            startAt = holder.getEndIndex();
            find = findFunction.apply(content, startAt);
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
                    } else if (target.matches(".*\\.[a-z]+")) {
                        fileName = target;
                        anchor = null;
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

    private static XrefHolder replaceHolder(XrefHolder holder, List<FileHolder> list, Path dir, FileHolder currentFile, Path currentRoot, boolean externalXrefAsText, Function<String, Optional<String>> attributeResolver) {
        String newFileName;
        String newAnchor;
        String fileName = holder.getFile();
        XrefHolderType type = holder.getType();
        if (fileName != null) {
            Path file;
            if (fileName.isEmpty()) {
                file = currentFile.getPath();
            } else {
                String subpath = resolveAttributes(fileName, attributeResolver);
                file = currentRoot.resolve(subpath);
                if (!Files.exists(file)) {
                    file = currentFile.getPath()
                            .getParent()
                            .resolve(fileName);
                }
                file = file.normalize();
            }
            Optional<FileHolder> findFile = findByFile(list, file);
            if (!findFile.isPresent()) {
                if (externalXrefAsText) {
                    type = XrefHolderType.TEXT;
                }
                newFileName = dir.relativize(file)
                        .toString()
                        .replace('\\', '/');
                newAnchor = holder.getAnchor();
            } else {
                newFileName = "";
                FileHolder fileHolder = findFile.get();
                newAnchor = computeNewAnchor(holder, fileHolder);
            }
        } else {
            newAnchor = computeNewAnchor(holder, currentFile);
            if (Objects.equals(newAnchor, holder.getAnchor())) {
                // no changes to the anchor, keep it unchanged:
                newFileName = null;
            } else {
                newFileName = "";
            }
        }
        return new XrefHolder(newFileName, newAnchor, holder.getText(), type, -1, -1);
    }

    static String resolveAttributes(String value, Function<String, Optional<String>> resolver) {
        return resolveAttributes(value, resolver, Collections.emptyList());
    }

    private static String resolveAttributes(String value, Function<String, Optional<String>> resolver, List<String> stack) {
        List<Range> ranges = SINGLE_CURLY_BRACKET_FINDER.findAll(value, false);
        int position = 0;
        StringBuilder sb = new StringBuilder();
        for (Range range : ranges) {
            sb.append(value.substring(position, range.getRangeStart()));
            String key = value.substring(range.getContentStart(), range.getContentEnd());
            List<String> newStack = new ArrayList<>(stack);
            newStack.add(key);
            if (stack.contains(key)) {
                throw new IllegalStateException("Can not evaluate the value of '{" + key + "}', because of following circular definition: {" + String.join("} -> {", newStack) + "}");
            }
            Optional<String> replacement = resolver.apply(key);
            if (replacement.isPresent()) {
                sb.append(resolveAttributes(replacement.get(), resolver, Collections.unmodifiableList(newStack)));
            } else {
                sb.append(value.substring(range.getRangeStart(), range.getRangeEnd()));
            }
            position = range.getRangeEnd();
        }
        sb.append(value.substring(position, value.length()));
        return sb.toString();
    }

    private static String computeNewAnchor(XrefHolder holder, FileHolder fileHolder) {
        String anchor;
        if (holder.getAnchor() == null || holder.getAnchor()
                .trim()
                .isEmpty()) {
            anchor = fileHolder.getFirstTitle()
                    .getTitleId();
        } else {
            String oldAnchor = holder.getAnchor()
                    .trim();
            if (fileHolder.getTitleAnchorMap()
                    .containsKey(oldAnchor)) {
                anchor = fileHolder.getTitleAnchorMap()
                        .get(oldAnchor);
            } else {
                anchor = oldAnchor;
            }
        }
        if (fileHolder.getAnchorShift()
                .containsKey(anchor)) {
            return fileHolder.getAnchorShift()
                    .get(anchor);
        } else {
            return anchor;
        }
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
                if (holder.getAnchor() != null) {
                    sb.append("#");
                }
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

    private static Optional<FileHolder> findByFile(List<FileHolder> list, Path file) {
        return list.stream()
                .filter(i -> Objects.equals(file.normalize(), i.getPath()))
                .findAny();
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

    public static String resolveAttributesInViewSourceLinkPattern(String viewSourceLinkPattern, Path file, Function<String, Optional<String>> attributeResolver) {
        Function<String, Optional<String>> resolver = (String placeholderName) -> {
            String placeholderLowerCase = placeholderName.toLowerCase();
            switch (placeholderLowerCase) {
            case "file-relative-to-git-repository":
                Optional<String> localGitRepositoryPath = attributeResolver.apply("local-git-repository-path");
                return relativePathResolver(placeholderName, localGitRepositoryPath, file);
            case "file-relative-to-gradle-projectdir":
                Optional<String> gradleProjectdir = attributeResolver.apply("gradle-projectdir");
                return relativePathResolver(placeholderName, gradleProjectdir, file);
            case "file-relative-to-gradle-rootdir":
                Optional<String> gradleRootdir = attributeResolver.apply("gradle-rootdir");
                return relativePathResolver(placeholderName, gradleRootdir, file);
            case "file-absolute-with-leading-slash":
                String absolutePath = PathUtil.normalizePath(file.toAbsolutePath());
                if (!absolutePath.startsWith("/")) {
                    return Optional.of("/" + absolutePath);
                }
                return Optional.of(absolutePath);
            default:
                return attributeResolver.apply(placeholderLowerCase);
            }
        };
        return resolveAttributes(viewSourceLinkPattern, resolver);
    }

    private static Optional<String> relativePathResolver(String placeholderName, Optional<String> folderPath, Path file) {
        if (folderPath.isPresent()) {
            return Optional.of(PathUtil.computeRelativePath(file, folderPath.get()));
        } else {
            return Optional.of("{" + placeholderName + "}");
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
