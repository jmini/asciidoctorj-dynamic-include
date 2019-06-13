package fr.jmini.asciidoctorj.dynamicinclude;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import fr.jmini.asciidoctorj.dynamicinclude.XrefHolder.XrefHolderType;

public class DynamicIncludeProcessorTest {

    @Test
    void testFindFiles() throws Exception {
        Path example1 = Paths.get("src/test/resources/example1");

        List<String> list101 = findFiles(example1, "pages/*.adoc", Collections.emptyList(), Collections.emptyList());
        assertThat(list101).containsExactly("pages/page1.adoc", "pages/page2.adoc", "pages/zpage.adoc");

        List<String> list102 = findFiles(example1, "**/*.adoc", Collections.emptyList(), Collections.emptyList());
        assertThat(list102).containsExactly("pages/page1.adoc", "pages/page2.adoc", "pages/zpage.adoc", "pub/pub.adoc", "pub/pub1.adoc");

        List<String> list103 = findFiles(example1.resolve("pages"), "*.adoc", Collections.emptyList(), Collections.emptyList());
        assertThat(list103).containsExactly("page1.adoc", "page2.adoc", "zpage.adoc");

        List<String> list104 = findFiles(example1, "pages/page*.adoc", Collections.emptyList(), Collections.emptyList());
        assertThat(list104).containsExactly("pages/page1.adoc", "pages/page2.adoc");

        List<String> list105 = findFiles(example1, "**/*.adoc", Collections.singletonList("pages"), Collections.emptyList());
        assertThat(list105).containsExactly("pages/page1.adoc", "pages/page2.adoc", "pages/zpage.adoc");

        List<String> list106 = findFiles(example1, "**/*.adoc", Collections.singletonList("xxx"), Collections.emptyList());
        assertThat(list106).isEmpty();

        List<String> list107 = findFiles(example1.resolve("pages"), "*.adoc", Collections.singletonList("xxx"), Collections.emptyList());
        assertThat(list107).isEmpty();

        List<String> list108 = findFiles(example1.resolve("pub"), "../pages/*.adoc", Collections.emptyList(), Collections.emptyList());
        assertThat(list108).containsExactly("../pages/page1.adoc", "../pages/page2.adoc", "../pages/zpage.adoc");

        List<String> list109 = findFiles(example1.toAbsolutePath(), "pages/*.adoc", Collections.emptyList(), Collections.emptyList());
        assertThat(list109).containsExactly("pages/page1.adoc", "pages/page2.adoc", "pages/zpage.adoc");

        List<String> list110 = findFiles(example1.resolve("pub")
                .toAbsolutePath(), "../pages/*.adoc", Collections.emptyList(), Collections.emptyList());
        assertThat(list110).containsExactly("../pages/page1.adoc", "../pages/page2.adoc", "../pages/zpage.adoc");

        Path example4 = Paths.get("src/test/resources/example4");

        List<String> list41 = findFiles(example4, "**/*.adoc", Collections.singletonList("scope1"), Collections.emptyList());
        assertThat(list41).containsExactly(
                "scope1/areaA/ipsum.adoc",
                "scope1/areaA/lorem.adoc",
                "scope1/areaB/main.adoc",
                "scope1/areaB/sub1/sub1.adoc",
                "scope1/areaB/sub2/sub2b.adoc");

        List<String> list42 = findFiles(example4, "**/*.adoc", Arrays.asList("scope1", "scope2"), Collections.emptyList());
        assertThat(list42).containsExactly(
                "scope1/areaA/ipsum.adoc",
                "scope1/areaA/lorem.adoc",
                "scope1/areaB/main.adoc",
                "scope1/areaB/sub1/sub1.adoc",
                "scope1/areaB/sub2/sub2b.adoc",
                "scope2/areaA/ipsum.adoc",
                "scope2/areaA/lorem.adoc",
                "scope2/areaC/areaC.adoc");

        List<String> list43 = findFiles(example4, "**/*.adoc", Arrays.asList("scope1", "scope2", "xxx"), Collections.emptyList());
        assertThat(list43).containsExactly(
                "scope1/areaA/ipsum.adoc",
                "scope1/areaA/lorem.adoc",
                "scope1/areaB/main.adoc",
                "scope1/areaB/sub1/sub1.adoc",
                "scope1/areaB/sub2/sub2b.adoc",
                "scope2/areaA/ipsum.adoc",
                "scope2/areaA/lorem.adoc",
                "scope2/areaC/areaC.adoc");

        List<String> list44 = findFiles(example4, "**/*.adoc", Arrays.asList("scope1", "scope2"), Collections.singletonList("xxx"));
        assertThat(list44).isEmpty();

        List<String> list45 = findFiles(example4, "**/*.adoc", Arrays.asList("scope1", "scope2"), Collections.singletonList("areaA"));
        assertThat(list45).containsExactly(
                "scope1/areaA/ipsum.adoc",
                "scope1/areaA/lorem.adoc",
                "scope2/areaA/ipsum.adoc",
                "scope2/areaA/lorem.adoc");

        List<String> list46 = findFiles(example4, "**/*.adoc", Arrays.asList("scope1", "scope2", "xxx"), Arrays.asList("areaA", "areaB"));
        assertThat(list46).containsExactly(
                "scope1/areaA/ipsum.adoc",
                "scope1/areaA/lorem.adoc",
                "scope1/areaB/main.adoc",
                "scope1/areaB/sub1/sub1.adoc",
                "scope1/areaB/sub2/sub2b.adoc",
                "scope2/areaA/ipsum.adoc",
                "scope2/areaA/lorem.adoc");

        List<String> list47 = findFiles(example4, "**/*.adoc", Arrays.asList("scope1", "scope2", "xxx"), Arrays.asList("areaA", "areaB", "xxx"));
        assertThat(list47).containsExactly(
                "scope1/areaA/ipsum.adoc",
                "scope1/areaA/lorem.adoc",
                "scope1/areaB/main.adoc",
                "scope1/areaB/sub1/sub1.adoc",
                "scope1/areaB/sub2/sub2b.adoc",
                "scope2/areaA/ipsum.adoc",
                "scope2/areaA/lorem.adoc");
    }

    private List<String> findFiles(Path dir, String glob, List<String> scopes, List<String> areas) throws IOException {
        List<String> list = DynamicIncludeProcessor.findFiles(dir, dir, glob, scopes, areas)
                .stream()
                .map(p -> dir.relativize(p)
                        .toString()
                        .replace('\\', '/'))
                .sorted()
                .collect(Collectors.toList());
        return list;
    }

    @Test
    void testConvertGlobToRegex() throws Exception {
        // star becomes dot star
        assertThat(DynamicIncludeProcessor.convertGlobToRegex("gl*b")).isEqualTo("gl.*b");

        // escaped star is unchanged
        assertThat(DynamicIncludeProcessor.convertGlobToRegex("gl\\*b")).isEqualTo("gl\\*b");

        // question mark becomes dot
        assertThat(DynamicIncludeProcessor.convertGlobToRegex("gl?b")).isEqualTo("gl.b");

        // escaped question mark is unchanged
        assertThat(DynamicIncludeProcessor.convertGlobToRegex("gl\\?b")).isEqualTo("gl\\?b");

        // character classes dont need conversion
        assertThat(DynamicIncludeProcessor.convertGlobToRegex("gl[-o]b")).isEqualTo("gl[-o]b");

        // escaped classes are unchanged
        assertThat(DynamicIncludeProcessor.convertGlobToRegex("gl\\[-o\\]b")).isEqualTo("gl\\[-o\\]b");

        // negation in character classes
        assertThat(DynamicIncludeProcessor.convertGlobToRegex("gl[!a-n!p-z]b")).isEqualTo("gl[^a-n!p-z]b");

        // nested negation in character classes
        assertThat(DynamicIncludeProcessor.convertGlobToRegex("gl[[!a-n]!p-z]b")).isEqualTo("gl[[^a-n]!p-z]b");

        // escape carat if it is the first char in a character class
        assertThat(DynamicIncludeProcessor.convertGlobToRegex("gl[^o]b")).isEqualTo("gl[\\^o]b");

        // meta chars are escaped
        assertThat(DynamicIncludeProcessor.convertGlobToRegex("gl?*.()+|^$@%b")).isEqualTo("gl..*\\.\\(\\)\\+\\|\\^\\$\\@\\%b");

        // meta chars in character classes don't need escaping
        assertThat(DynamicIncludeProcessor.convertGlobToRegex("gl[?*.()+|^$@%]b")).isEqualTo("gl[?*.()+|^$@%]b");

        // escaped backslash is unchanged
        assertThat(DynamicIncludeProcessor.convertGlobToRegex("gl\\\\b")).isEqualTo("gl\\\\b");

        // slashQ and slashE are escaped
        assertThat(DynamicIncludeProcessor.convertGlobToRegex("\\Qglob\\E")).isEqualTo("\\\\Qglob\\\\E");

        // braces are turned into groups
        assertThat(DynamicIncludeProcessor.convertGlobToRegex("{glob,regex}")).isEqualTo("(glob|regex)");

        // escaped braces are unchanged
        assertThat(DynamicIncludeProcessor.convertGlobToRegex("\\{glob\\}")).isEqualTo("\\{glob\\}");

        // commas dont need escaping
        assertThat(DynamicIncludeProcessor.convertGlobToRegex("{glob\\,regex},")).isEqualTo("(glob,regex),");
    }

    @Test
    void testComparator() throws Exception {
        List<Integer> input = Arrays.asList(10, 24, 52, 3, 43, 91);
        Function<Integer, String> keyExtractor = i -> i.toString();

        List<String> order1 = Collections.emptyList();
        MessageCollector collector1 = new MessageCollector();
        Comparator<Integer> comparator1 = DynamicIncludeProcessor.getOrderedKeyPatternComparator(collector1, input, order1, i -> i.toString());
        List<Integer> list1 = sortIntegerList(input, comparator1, keyExtractor);
        assertThat(list1).containsExactly(10, 24, 3, 43, 52, 91);
        assertThat(collector1.getMessages()).isEmpty();

        List<String> order2 = Arrays.asList("3", "10", "91", "52", "43", "24");
        MessageCollector collector2 = new MessageCollector();
        Comparator<Integer> comparator2 = DynamicIncludeProcessor.getOrderedKeyPatternComparator(collector2, input, order2, keyExtractor);
        List<Integer> list2 = sortIntegerList(input, comparator2, keyExtractor);
        assertThat(list2).containsExactly(3, 10, 91, 52, 43, 24);
        assertThat(collector2.getMessages()).isEmpty();

        List<String> order3 = Arrays.asList("91", "3");
        MessageCollector collector3 = new MessageCollector();
        Comparator<Integer> comparator3 = DynamicIncludeProcessor.getOrderedKeyPatternComparator(collector3, input, order3, keyExtractor);
        List<Integer> list3 = sortIntegerList(input, comparator3, keyExtractor);
        assertThat(list3).containsExactly(91, 3, 10, 24, 43, 52);
        assertThat(collector3.getMessages()).containsExactly(
                "Did not find any information order for '10', putting it at the end of the document",
                "Did not find any information order for '24', putting it at the end of the document",
                "Did not find any information order for '52', putting it at the end of the document",
                "Did not find any information order for '43', putting it at the end of the document");

        List<String> order4 = Arrays.asList("[0-9]", "[0-9]+");
        MessageCollector collector4 = new MessageCollector();
        Comparator<Integer> comparator4 = DynamicIncludeProcessor.getOrderedKeyPatternComparator(collector4, input, order4, keyExtractor);
        List<Integer> list4 = sortIntegerList(input, comparator4, keyExtractor);
        assertThat(list4).containsExactly(3, 10, 24, 43, 52, 91);
        assertThat(collector4.getMessages()).isEmpty();

        List<String> order5 = Arrays.asList("3", "10", "91", "52", "43", "24");
        Comparator<Integer> comparator5 = DynamicIncludeProcessor.getOrderedValuesComparator(input, order5, keyExtractor, keyExtractor);
        List<Integer> list5 = sortIntegerList(input, comparator5, keyExtractor);
        assertThat(list5).containsExactly(3, 10, 91, 52, 43, 24);

        List<String> order6 = Arrays.asList("91", "3");
        Comparator<Integer> comparator6 = DynamicIncludeProcessor.getOrderedValuesComparator(input, order6, keyExtractor, keyExtractor);
        List<Integer> list6 = sortIntegerList(input, comparator6, keyExtractor);
        assertThat(list6).containsExactly(91, 3, 10, 24, 43, 52);
    }

    private List<Integer> sortIntegerList(List<Integer> list, Comparator<Integer> comparator, Function<Integer, String> keyExtractor) {
        return list.stream()
                .sorted(comparator.thenComparing(Comparator.comparing(keyExtractor)))
                .collect(Collectors.toList());
    }

    @Test
    void testSortList() throws Exception {
        Path dir = Paths.get("dir");
        List<FileHolder> input = Arrays.asList(
                createFileHolder(dir, "s1/a1/page8.adoc", "s1", "a1"),
                createFileHolder(dir, "s2/a2/page2.adoc", "s2", "a2"),
                createFileHolder(dir, "s1/a3/pageB.adoc", "s1", "a3"),
                createFileHolder(dir, "s2/a1/page3.adoc", "s2", "a1"),
                createFileHolder(dir, "s1/a2/pageA.adoc", "s1", "a2"),
                createFileHolder(dir, "s2/a3/pageC.adoc", "s2", "a3"));

        MessageCollector collector1 = new MessageCollector();
        List<String> list1 = sortList(input, collector1, Collections.emptyList(), Arrays.asList("s1", "s2"), Collections.emptyList());
        assertThat(list1).containsExactly(
                "s1/a1/page8.adoc",
                "s1/a2/pageA.adoc",
                "s1/a3/pageB.adoc",
                "s2/a1/page3.adoc",
                "s2/a2/page2.adoc",
                "s2/a3/pageC.adoc");
        assertThat(collector1.getMessages()).isEmpty();

        MessageCollector collector2 = new MessageCollector();
        List<String> list2 = sortList(input, collector2, Collections.emptyList(), Arrays.asList("s1", "s2"), Arrays.asList("a2", "a1", "xx", "a3"));
        assertThat(list2).containsExactly(
                "s1/a2/pageA.adoc",
                "s1/a1/page8.adoc",
                "s1/a3/pageB.adoc",
                "s2/a2/page2.adoc",
                "s2/a1/page3.adoc",
                "s2/a3/pageC.adoc");
        assertThat(collector2.getMessages()).isEmpty();

        MessageCollector collector3 = new MessageCollector();
        List<String> list3 = sortList(input, collector3, Collections.singletonList("s[0-9]\\/a[0-9]\\/page[A-Z].adoc"), Arrays.asList("s2", "s1"), Arrays.asList("a3", "a1", "a2"));
        assertThat(list3).containsExactly(
                "s2/a3/pageC.adoc",
                "s1/a3/pageB.adoc",
                "s1/a2/pageA.adoc",
                "s2/a1/page3.adoc",
                "s2/a2/page2.adoc",
                "s1/a1/page8.adoc");
        assertThat(collector3.getMessages()).containsExactly(
                "Did not find any information order for 's1/a1/page8.adoc', putting it at the end of the document",
                "Did not find any information order for 's2/a2/page2.adoc', putting it at the end of the document",
                "Did not find any information order for 's2/a1/page3.adoc', putting it at the end of the document");
    }

    private List<String> sortList(List<FileHolder> input, MessageCollector collector, List<String> patternOrder, List<String> scopesOrder, List<String> areasOrder) {
        List<String> list1 = DynamicIncludeProcessor.sortList(collector, input, patternOrder, scopesOrder, areasOrder)
                .stream()
                .map(FileHolder::getKey)
                .collect(Collectors.toList());
        return list1;
    }

    private FileHolder createFileHolder(Path dir, String subPath, String scope, String area) {
        Path page = dir.resolve(subPath);
        return new FileHolder(page, subPath, scope, area, "!! some content !!", TitleType.ABSENT, null, 0, null, 100, 101);
    }

    @Test
    void testfindFilesAndSort() throws Exception {
        MessageCollector collector1 = new MessageCollector();
        List<String> list1 = findFilesAndSort(collector1, Collections.emptyList());
        assertThat(list1).containsExactly(
                "scope1/areaA/ipsum.adoc",
                "scope1/areaA/lorem.adoc",
                "scope1/areaB/main.adoc",
                "scope1/areaB/sub1/sub1.adoc",
                "scope1/areaB/sub2/sub2b.adoc",
                "scope2/areaA/ipsum.adoc",
                "scope2/areaA/lorem.adoc");
        assertThat(collector1.getMessages()).isEmpty();

        MessageCollector collector2 = new MessageCollector();
        List<String> list2 = findFilesAndSort(collector2, Arrays.asList(
                "scope1/areaA/ipsum.adoc",
                "scope2/areaA/ipsum.adoc",
                "scope1/areaA/lorem.adoc",
                "scope2/areaA/lorem.adoc",
                "scope1/areaB/sub2/sub2b.adoc",
                "scope1/areaB/sub1/sub1.adoc",
                "scope1/areaB/main.adoc"));
        assertThat(list2).containsExactly(
                "scope1/areaA/ipsum.adoc",
                "scope2/areaA/ipsum.adoc",
                "scope1/areaA/lorem.adoc",
                "scope2/areaA/lorem.adoc",
                "scope1/areaB/sub2/sub2b.adoc",
                "scope1/areaB/sub1/sub1.adoc",
                "scope1/areaB/main.adoc");
        assertThat(collector2.getMessages()).isEmpty();

        MessageCollector collector3 = new MessageCollector();
        List<String> list3 = findFilesAndSort(collector3, Arrays.asList(
                "scope2/*/*.adoc",
                "scope1/*/*/*.adoc",
                "scope1/*/*.adoc"));
        assertThat(list3).containsExactly(
                "scope2/areaA/ipsum.adoc",
                "scope2/areaA/lorem.adoc",
                "scope1/areaB/sub1/sub1.adoc",
                "scope1/areaB/sub2/sub2b.adoc",
                "scope1/areaA/ipsum.adoc",
                "scope1/areaA/lorem.adoc",
                "scope1/areaB/main.adoc");
        assertThat(collector3.getMessages()).isEmpty();

        MessageCollector collector4 = new MessageCollector();
        List<String> list4 = findFilesAndSort(collector4, Arrays.asList(
                "scope2/*",
                "scope1/*"));
        assertThat(list4).containsExactly(
                "scope2/areaA/ipsum.adoc",
                "scope2/areaA/lorem.adoc",
                "scope1/areaA/ipsum.adoc",
                "scope1/areaA/lorem.adoc",
                "scope1/areaB/main.adoc",
                "scope1/areaB/sub1/sub1.adoc",
                "scope1/areaB/sub2/sub2b.adoc");
        assertThat(collector4.getMessages()).isEmpty();

        MessageCollector collector5 = new MessageCollector();
        List<String> list5 = findFilesAndSort(collector5, Arrays.asList(
                "*/areaB/*",
                "*/areaA/*"));
        assertThat(list5).containsExactly(
                "scope1/areaB/main.adoc",
                "scope1/areaB/sub1/sub1.adoc",
                "scope1/areaB/sub2/sub2b.adoc",
                "scope1/areaA/ipsum.adoc",
                "scope1/areaA/lorem.adoc",
                "scope2/areaA/ipsum.adoc",
                "scope2/areaA/lorem.adoc");
        assertThat(collector5.getMessages()).isEmpty();

    }

    private List<String> findFilesAndSort(MessageCollector collector, List<String> sortOrder) throws IOException {
        Path example4 = Paths.get("src/test/resources/example4");
        List<Path> list = DynamicIncludeProcessor.findFiles(example4, example4, "**/*.adoc", Arrays.asList("scope1", "scope2"), Arrays.asList("areaA", "areaB"));
        List<String> order = sortOrder.stream()
                .map(DynamicIncludeProcessor::convertGlobToRegex)
                .collect(Collectors.toList());
        Function<Path, String> toKey = p -> example4.relativize(p)
                .toString()
                .replace('\\', '/');
        Comparator<Path> comparator = DynamicIncludeProcessor.getOrderedKeyPatternComparator(collector, list, order, toKey);
        return list.stream()
                .sorted(comparator.thenComparing(Comparator.comparing(toKey)))
                .map(toKey)
                .collect(Collectors.toList());
    }

    @Test
    void testReplaceXrefDoubleAngledBracketLinks() throws Exception {
        Path dir = Paths.get("dir");
        Path page1 = dir.resolve("folder/page.adoc");
        Path page2 = dir.resolve("folder/other.adoc");
        FileHolder holder1 = new FileHolder(page1, "folder/page.adoc", null, null, "!! dummy content !!", TitleType.PRESENT, "Page 1", 2, "_page_1", 91, 95);
        FileHolder holder2 = new FileHolder(page2, "folder/other.adoc", null, null, "!! dummy content !!", TitleType.PRESENT, "Other Page", 2, "_other_page", 101, 105);
        List<FileHolder> list = Arrays.asList(holder1, holder2);

        String emptyList = DynamicIncludeProcessor.replaceXrefDoubleAngledBracketLinks("Some content", Collections.emptyList(), dir, page1, dir, true);
        assertThat(emptyList).isEqualTo("Some content");

        String oneElement = DynamicIncludeProcessor.replaceXrefDoubleAngledBracketLinks("Some content", Collections.singletonList(holder1), dir, page1, dir, true);
        assertThat(oneElement).isEqualTo("Some content");

        String page2LinkStart = DynamicIncludeProcessor.replaceXrefDoubleAngledBracketLinks("<<other.adoc#test, other>> some link", list, dir, page1, dir, true);
        assertThat(page2LinkStart).isEqualTo("<<#test, other>> some link");

        String page2Link = DynamicIncludeProcessor.replaceXrefDoubleAngledBracketLinks("Some <<other.adoc#test, other>> link", list, dir, page1, dir, true);
        assertThat(page2Link).isEqualTo("Some <<#test, other>> link");

        String page2LinkEnd = DynamicIncludeProcessor.replaceXrefDoubleAngledBracketLinks("Some link <<other.adoc#test, other>>", list, dir, page1, dir, true);
        assertThat(page2LinkEnd).isEqualTo("Some link <<#test, other>>");

        String internalLink = DynamicIncludeProcessor.replaceXrefDoubleAngledBracketLinks("Some <<test, internal>> link", list, dir, page1, dir, true);
        assertThat(internalLink).isEqualTo("Some <<test, internal>> link");

        String externalLink = DynamicIncludeProcessor.replaceXrefDoubleAngledBracketLinks("Some <<ext.adoc#test, other>> link", list, dir, page1, dir, false);
        assertThat(externalLink).isEqualTo("Some <<folder/ext.adoc#test, other>> link");

        String rootLink = DynamicIncludeProcessor.replaceXrefDoubleAngledBracketLinks("Some <<{root}folder/other.adoc#test, root>> link", list, dir, page1, dir, true);
        assertThat(rootLink).isEqualTo("Some <<#test, root>> link");

        String root2Link = DynamicIncludeProcessor.replaceXrefDoubleAngledBracketLinks("Some <<{root}folder/other.adoc#test, root>> link", list, dir, page1, dir.resolve("folder/.."), true);
        assertThat(root2Link).isEqualTo("Some <<#test, root>> link");

        String rootLinkNoAnchor = DynamicIncludeProcessor.replaceXrefDoubleAngledBracketLinks("Some <<{root}folder/other.adoc#, root>> link", list, dir, page1, dir, true);
        assertThat(rootLinkNoAnchor).isEqualTo("Some <<#_other_page, root>> link");
    }

    @Test
    void testReplaceXrefInlineLinks() throws Exception {
        Path dir = Paths.get("dir");
        Path page1 = dir.resolve("folder/page.adoc");
        Path page2 = dir.resolve("folder/other.adoc");
        FileHolder holder1 = new FileHolder(page1, "folder/page.adoc", null, null, "!! dummy content !!", TitleType.PRESENT, "Page 1", 2, "_page_1", 91, 95);
        FileHolder holder2 = new FileHolder(page2, "folder/other.adoc", null, null, "!! dummy content !!", TitleType.PRESENT, "Other Page", 2, "_other_page", 101, 105);
        List<FileHolder> list = Arrays.asList(holder1, holder2);

        String emptyList = DynamicIncludeProcessor.replaceXrefInlineLinks("Some content", Collections.emptyList(), dir, page1, dir, true);
        assertThat(emptyList).isEqualTo("Some content");

        String oneElement = DynamicIncludeProcessor.replaceXrefInlineLinks("Some content", Collections.singletonList(holder1), dir, page1, dir, true);
        assertThat(oneElement).isEqualTo("Some content");

        String page2LinkStart = DynamicIncludeProcessor.replaceXrefInlineLinks("xref:other.adoc#test[other] some link", list, dir, page1, dir, true);
        assertThat(page2LinkStart).isEqualTo("xref:#test[other] some link");

        String page2Link = DynamicIncludeProcessor.replaceXrefInlineLinks("Some xref:other.adoc#test[other] link", list, dir, page1, dir, true);
        assertThat(page2Link).isEqualTo("Some xref:#test[other] link");

        String page2LinkEnd = DynamicIncludeProcessor.replaceXrefInlineLinks("Some link xref:other.adoc#test[other]", list, dir, page1, dir, true);
        assertThat(page2LinkEnd).isEqualTo("Some link xref:#test[other]");

        String internalLink = DynamicIncludeProcessor.replaceXrefDoubleAngledBracketLinks("Some xref:#test[internal] link", list, dir, page1, dir, true);
        assertThat(internalLink).isEqualTo("Some xref:#test[internal] link");

        String externalLink = DynamicIncludeProcessor.replaceXrefInlineLinks("Some xref:ext.adoc#[other] link", list, dir, page1, dir, false);
        assertThat(externalLink).isEqualTo("Some xref:folder/ext.adoc#[other] link");

        String externalWithAnchorLink = DynamicIncludeProcessor.replaceXrefInlineLinks("Some xref:ext.adoc#test[other] link", list, dir, page1, dir, false);
        assertThat(externalWithAnchorLink).isEqualTo("Some xref:folder/ext.adoc#test[other] link");

        String externalLinkReplacedToText = DynamicIncludeProcessor.replaceXrefInlineLinks("Some xref:ext.adoc#[other] link", list, dir, page1, dir, true);
        assertThat(externalLinkReplacedToText).isEqualTo("Some other link");

        String externalWithAnchorLinkReplacedToText = DynamicIncludeProcessor.replaceXrefInlineLinks("Some xref:ext.adoc#test[other] link", list, dir, page1, dir, true);
        assertThat(externalWithAnchorLinkReplacedToText).isEqualTo("Some other link");

        String rootLink = DynamicIncludeProcessor.replaceXrefInlineLinks("Some xref:{root}folder/other.adoc#test[root] link", list, dir, page1, dir, true);
        assertThat(rootLink).isEqualTo("Some xref:#test[root] link");

        String rootLinkNoAnchor = DynamicIncludeProcessor.replaceXrefInlineLinks("Some xref:{root}folder/other.adoc#[root] link", list, dir, page1, dir, true);
        assertThat(rootLinkNoAnchor).isEqualTo("Some xref:#_other_page[root] link");

        String rootLinkEmptyAnchor = DynamicIncludeProcessor.replaceXrefInlineLinks("Some xref:{root}folder/other.adoc#[root] link", list, dir, page1, dir, true);
        assertThat(rootLinkEmptyAnchor).isEqualTo("Some xref:#_other_page[root] link");
    }

    @Test
    void testFindNextXrefDoubleAngledBracket() throws Exception {
        Optional<XrefHolder> emptyList = DynamicIncludeProcessor.findNextXrefDoubleAngledBracket("Some content", 0);
        assertThat(emptyList).isNotPresent();

        Optional<XrefHolder> linkStartOpt = DynamicIncludeProcessor.findNextXrefDoubleAngledBracket("<<other.adoc#test, other>> some link", 0);
        assertThat(linkStartOpt).isPresent();
        XrefHolder linkStart = linkStartOpt.get();
        assertThat(linkStart.getType()).isEqualTo(XrefHolderType.DOUBLE_ANGLED_BRACKET);
        assertThat(linkStart.getStartIndex()).isEqualTo(0);
        assertThat(linkStart.getEndIndex()).isEqualTo(26);
        assertThat(linkStart.getFile()).isEqualTo("other.adoc");
        assertThat(linkStart.getAnchor()).isEqualTo("test");
        assertThat(linkStart.getText()).isEqualTo(" other");

        Optional<XrefHolder> linkOpt = DynamicIncludeProcessor.findNextXrefDoubleAngledBracket("Some <<page1.adoc#sect, Other>> link", 0);
        assertThat(linkOpt).isPresent();
        XrefHolder link = linkOpt.get();
        assertThat(link.getType()).isEqualTo(XrefHolderType.DOUBLE_ANGLED_BRACKET);
        assertThat(link.getStartIndex()).isEqualTo(5);
        assertThat(link.getEndIndex()).isEqualTo(31);
        assertThat(link.getFile()).isEqualTo("page1.adoc");
        assertThat(link.getAnchor()).isEqualTo("sect");
        assertThat(link.getText()).isEqualTo(" Other");

        Optional<XrefHolder> linkEndOpt = DynamicIncludeProcessor.findNextXrefDoubleAngledBracket("Some link <<page2.adoc#here, OTHER>>", 0);
        assertThat(linkEndOpt).isPresent();
        XrefHolder linkEnd = linkEndOpt.get();
        assertThat(linkEnd.getType()).isEqualTo(XrefHolderType.DOUBLE_ANGLED_BRACKET);
        assertThat(linkEnd.getStartIndex()).isEqualTo(10);
        assertThat(linkEnd.getEndIndex()).isEqualTo(36);
        assertThat(linkEnd.getFile()).isEqualTo("page2.adoc");
        assertThat(linkEnd.getAnchor()).isEqualTo("here");
        assertThat(linkEnd.getText()).isEqualTo(" OTHER");

        Optional<XrefHolder> internalLinkOpt = DynamicIncludeProcessor.findNextXrefDoubleAngledBracket("Some <<test, internal>> link", 0);
        assertThat(internalLinkOpt).isPresent();
        XrefHolder internalLink = internalLinkOpt.get();
        assertThat(internalLink.getType()).isEqualTo(XrefHolderType.DOUBLE_ANGLED_BRACKET);
        assertThat(internalLink.getStartIndex()).isEqualTo(5);
        assertThat(internalLink.getEndIndex()).isEqualTo(23);
        assertThat(internalLink.getFile()).isNull();
        assertThat(internalLink.getAnchor()).isEqualTo("test");
        assertThat(internalLink.getText()).isEqualTo(" internal");

        Optional<XrefHolder> internalNoTextOpt = DynamicIncludeProcessor.findNextXrefDoubleAngledBracket("Some <<test>> link", 0);
        assertThat(internalNoTextOpt).isPresent();
        XrefHolder internalNoText = internalNoTextOpt.get();
        assertThat(internalNoText.getType()).isEqualTo(XrefHolderType.DOUBLE_ANGLED_BRACKET);
        assertThat(internalNoText.getStartIndex()).isEqualTo(5);
        assertThat(internalNoText.getEndIndex()).isEqualTo(13);
        assertThat(internalNoText.getFile()).isNull();
        assertThat(internalNoText.getAnchor()).isEqualTo("test");
        assertThat(internalNoText.getText()).isNull();

        Optional<XrefHolder> noTextOpt = DynamicIncludeProcessor.findNextXrefDoubleAngledBracket("Some <<other.adoc#test>> link", 0);
        assertThat(noTextOpt).isPresent();
        XrefHolder noText = noTextOpt.get();
        assertThat(noText.getType()).isEqualTo(XrefHolderType.DOUBLE_ANGLED_BRACKET);
        assertThat(noText.getStartIndex()).isEqualTo(5);
        assertThat(noText.getEndIndex()).isEqualTo(24);
        assertThat(noText.getFile()).isEqualTo("other.adoc");
        assertThat(noText.getAnchor()).isEqualTo("test");
        assertThat(noText.getText()).isNull();
    }

    @Test
    void testFindNextXrefInline() throws Exception {
        Optional<XrefHolder> emptyList = DynamicIncludeProcessor.findNextXrefInline("Some content", 0);
        assertThat(emptyList).isNotPresent();

        Optional<XrefHolder> linkStartOpt = DynamicIncludeProcessor.findNextXrefInline("xref:other.adoc#test[other] some link", 0);
        assertThat(linkStartOpt).isPresent();
        XrefHolder linkStart = linkStartOpt.get();
        assertThat(linkStart.getType()).isEqualTo(XrefHolderType.INLINE);
        assertThat(linkStart.getStartIndex()).isEqualTo(0);
        assertThat(linkStart.getEndIndex()).isEqualTo(27);
        assertThat(linkStart.getFile()).isEqualTo("other.adoc");
        assertThat(linkStart.getAnchor()).isEqualTo("test");
        assertThat(linkStart.getText()).isEqualTo("other");

        Optional<XrefHolder> linkOpt = DynamicIncludeProcessor.findNextXrefInline("Some xref:page1.adoc#sect[Other] link", 0);
        assertThat(linkOpt).isPresent();
        XrefHolder link = linkOpt.get();
        assertThat(link.getType()).isEqualTo(XrefHolderType.INLINE);
        assertThat(link.getStartIndex()).isEqualTo(5);
        assertThat(link.getEndIndex()).isEqualTo(32);
        assertThat(link.getFile()).isEqualTo("page1.adoc");
        assertThat(link.getAnchor()).isEqualTo("sect");
        assertThat(link.getText()).isEqualTo("Other");

        Optional<XrefHolder> linkEndOpt = DynamicIncludeProcessor.findNextXrefInline("Some link xref:page2.adoc#here[OTHER]", 0);
        assertThat(linkEndOpt).isPresent();
        XrefHolder linkEnd = linkEndOpt.get();
        assertThat(linkEnd.getType()).isEqualTo(XrefHolderType.INLINE);
        assertThat(linkEnd.getStartIndex()).isEqualTo(10);
        assertThat(linkEnd.getEndIndex()).isEqualTo(37);
        assertThat(linkEnd.getFile()).isEqualTo("page2.adoc");
        assertThat(linkEnd.getAnchor()).isEqualTo("here");
        assertThat(linkEnd.getText()).isEqualTo("OTHER");

        Optional<XrefHolder> internalLinkOpt = DynamicIncludeProcessor.findNextXrefInline("Some xref:test[internal] link", 0);
        assertThat(internalLinkOpt).isPresent();
        XrefHolder internalLink = internalLinkOpt.get();
        assertThat(internalLink.getType()).isEqualTo(XrefHolderType.INLINE);
        assertThat(internalLink.getStartIndex()).isEqualTo(5);
        assertThat(internalLink.getEndIndex()).isEqualTo(24);
        assertThat(internalLink.getFile()).isNull();
        assertThat(internalLink.getAnchor()).isEqualTo("test");
        assertThat(internalLink.getText()).isEqualTo("internal");

        Optional<XrefHolder> internalNoTextOpt = DynamicIncludeProcessor.findNextXrefInline("Some xref:test[] link", 0);
        assertThat(internalNoTextOpt).isPresent();
        XrefHolder internalNoText = internalNoTextOpt.get();
        assertThat(internalNoText.getType()).isEqualTo(XrefHolderType.INLINE);
        assertThat(internalNoText.getStartIndex()).isEqualTo(5);
        assertThat(internalNoText.getEndIndex()).isEqualTo(16);
        assertThat(internalNoText.getFile()).isNull();
        assertThat(internalNoText.getAnchor()).isEqualTo("test");
        assertThat(internalNoText.getText()).isNull();

        Optional<XrefHolder> noTextOpt = DynamicIncludeProcessor.findNextXrefInline("Some xref:other.adoc#test[] link", 0);
        assertThat(noTextOpt).isPresent();
        XrefHolder noText = noTextOpt.get();
        assertThat(noText.getType()).isEqualTo(XrefHolderType.INLINE);
        assertThat(noText.getStartIndex()).isEqualTo(5);
        assertThat(noText.getEndIndex()).isEqualTo(27);
        assertThat(noText.getFile()).isEqualTo("other.adoc");
        assertThat(noText.getAnchor()).isEqualTo("test");
        assertThat(noText.getText()).isNull();
    }

    @Test
    void testHolderToAsciiDoc() throws Exception {
        // DoubleAngledBracket:
        String input10 = "<<other.adoc#test, other>>";
        Optional<XrefHolder> findHolder10 = DynamicIncludeProcessor.findNextXrefDoubleAngledBracket(input10, 0);
        assertThat(findHolder10).isPresent();
        XrefHolder holder10 = findHolder10.get();
        String string10 = DynamicIncludeProcessor.holderToAsciiDoc(holder10);
        assertThat(string10).isEqualTo(input10);

        String input11 = "<<test,internal>>";
        Optional<XrefHolder> findHolder11 = DynamicIncludeProcessor.findNextXrefDoubleAngledBracket(input11, 0);
        assertThat(findHolder11).isPresent();
        XrefHolder holder11 = findHolder11.get();
        String string11 = DynamicIncludeProcessor.holderToAsciiDoc(holder11);
        assertThat(string11).isEqualTo(input11);

        String input12 = "<<here>>";
        Optional<XrefHolder> findHolder12 = DynamicIncludeProcessor.findNextXrefDoubleAngledBracket(input12, 0);
        assertThat(findHolder12).isPresent();
        XrefHolder holder12 = findHolder12.get();
        String string12 = DynamicIncludeProcessor.holderToAsciiDoc(holder12);
        assertThat(string12).isEqualTo(input12);

        String input13 = "<<other.adoc#test>>";
        Optional<XrefHolder> findHolder13 = DynamicIncludeProcessor.findNextXrefDoubleAngledBracket(input13, 0);
        assertThat(findHolder13).isPresent();
        XrefHolder holder13 = findHolder13.get();
        String string13 = DynamicIncludeProcessor.holderToAsciiDoc(holder13);
        assertThat(string13).isEqualTo(input13);

        String input14 = "<<other.adoc#>>";
        Optional<XrefHolder> findHolder14 = DynamicIncludeProcessor.findNextXrefDoubleAngledBracket(input14, 0);
        assertThat(findHolder14).isPresent();
        XrefHolder holder14 = findHolder14.get();
        String string14 = DynamicIncludeProcessor.holderToAsciiDoc(holder14);
        assertThat(string14).isEqualTo(input14);

        String input15 = "<<other.adoc#, here>>";
        Optional<XrefHolder> findHolder15 = DynamicIncludeProcessor.findNextXrefDoubleAngledBracket(input15, 0);
        assertThat(findHolder15).isPresent();
        XrefHolder holder15 = findHolder15.get();
        String string15 = DynamicIncludeProcessor.holderToAsciiDoc(holder15);
        assertThat(string15).isEqualTo(input15);

        // Inline:
        String input20 = "xref:other.adoc#test[other]";
        Optional<XrefHolder> findHolder20 = DynamicIncludeProcessor.findNextXrefInline(input20, 0);
        assertThat(findHolder20).isPresent();
        XrefHolder holder20 = findHolder20.get();
        String string20 = DynamicIncludeProcessor.holderToAsciiDoc(holder20);
        assertThat(string20).isEqualTo(input20);

        String input21 = "xref:test[internal]";
        Optional<XrefHolder> findHolder21 = DynamicIncludeProcessor.findNextXrefInline(input21, 0);
        assertThat(findHolder21).isPresent();
        XrefHolder holder21 = findHolder21.get();
        String string21 = DynamicIncludeProcessor.holderToAsciiDoc(holder21);
        assertThat(string21).isEqualTo(input21);

        String input22 = "xref:here[]";
        Optional<XrefHolder> findHolder22 = DynamicIncludeProcessor.findNextXrefInline(input22, 0);
        assertThat(findHolder22).isPresent();
        XrefHolder holder22 = findHolder22.get();
        String string22 = DynamicIncludeProcessor.holderToAsciiDoc(holder22);
        assertThat(string22).isEqualTo(input22);

        String input23 = "xref:other.adoc#test[]";
        Optional<XrefHolder> findHolder23 = DynamicIncludeProcessor.findNextXrefInline(input23, 0);
        assertThat(findHolder23).isPresent();
        XrefHolder holder23 = findHolder23.get();
        String string23 = DynamicIncludeProcessor.holderToAsciiDoc(holder23);
        assertThat(string23).isEqualTo(input23);

        String input24 = "xref:other.adoc#[]";
        Optional<XrefHolder> findHolder24 = DynamicIncludeProcessor.findNextXrefInline(input24, 0);
        assertThat(findHolder24).isPresent();
        XrefHolder holder24 = findHolder24.get();
        String string24 = DynamicIncludeProcessor.holderToAsciiDoc(holder24);
        assertThat(string24).isEqualTo(input24);

        String input25 = "xref:other.adoc#[here]";
        Optional<XrefHolder> findHolder25 = DynamicIncludeProcessor.findNextXrefInline(input25, 0);
        assertThat(findHolder25).isPresent();
        XrefHolder holder25 = findHolder25.get();
        String string25 = DynamicIncludeProcessor.holderToAsciiDoc(holder25);
        assertThat(string25).isEqualTo(input25);

        // Text:
        XrefHolder holder = new XrefHolder("file", "anchor", "text", XrefHolderType.TEXT, -1, -1);
        String string = DynamicIncludeProcessor.holderToAsciiDoc(holder);
        assertThat(string).isEqualTo("text");
    }

    @Test
    void testCreateFileHolder() throws Exception {
        Path dir = Paths.get("src/test/resources")
                .toAbsolutePath();

        Path path1 = dir.resolve("example2/content/content1.adoc");
        FileHolder holder1 = DynamicIncludeProcessor.createFileHolder(dir, dir, path1, "_", "_");
        assertThat(holder1.getKey()).isEqualTo("example2/content/content1.adoc");
        assertThat(holder1.getTitleType()).isEqualTo(TitleType.PRESENT);
        assertThat(holder1.getTitle()).isEqualTo("Content 1");
        assertThat(holder1.getTitleEnd()).isGreaterThan(90);

        Path path2 = dir.resolve("example1/pages/page2.adoc");
        FileHolder holder2 = DynamicIncludeProcessor.createFileHolder(dir, dir, path2, "_", "_");
        assertThat(holder2.getKey()).isEqualTo("example1/pages/page2.adoc");
        assertThat(holder2.getTitleType()).isEqualTo(TitleType.ABSENT);
        assertThat(holder2.getTitleEnd()).isEqualTo(0);

        Path path3 = dir.resolve("page.adoc");
        FileHolder holder3 = DynamicIncludeProcessor.createFileHolder(dir, dir, path3, "_", "_");
        assertThat(holder3.getKey()).isEqualTo("page.adoc");
        assertThat(holder3.getTitleType()).isEqualTo(TitleType.COMMENTED);
        assertThat(holder3.getTitle()).isEqualTo("Page Test");
        assertThat(holder3.getTitleEnd()).isEqualTo(15);

    }

    @Test
    void testComputeTitleId() throws Exception {
        assertThat(DynamicIncludeProcessor.computeTitleId("My Title", "", "-")).isEqualTo("my-title");
        assertThat(DynamicIncludeProcessor.computeTitleId("My Title", "_", "_")).isEqualTo("_my_title");

        assertThat(DynamicIncludeProcessor.computeTitleId("pages/content1.adoc", "", "-")).isEqualTo("pagescontent1-adoc");
        assertThat(DynamicIncludeProcessor.computeTitleId("pages/content1.adoc", "_", "_")).isEqualTo("_pagescontent1_adoc");
    }

    @Test
    void testCountLines() throws Exception {
        assertThat(DynamicIncludeProcessor.countLines("one\ntwo")).isEqualTo(2);
        assertThat(DynamicIncludeProcessor.countLines("one")).isEqualTo(1);
    }

    @Test
    void testReplacePlaceholders() throws Exception {
        Path file = Paths.get("/some/path/file.txt");
        Path file2 = Paths.get(URI.create("file:///C:/some/path/file.txt"));
        Function<String, Optional<String>> attributeGetter = (String s) -> {
            switch (s) {
            case "foo":
                return Optional.of("bar");
            case "local-git-repository-path":
                return Optional.of("/some/path/here/../");
            case "gradle-projectdir":
                return Optional.of("/some/test/../path/");
            case "gradle-rootdir":
                return Optional.of("/some/");
            }
            return Optional.empty();
        };

        assertThat(DynamicIncludeProcessor.replacePlaceholders("xxx", file, attributeGetter)).isEqualTo("xxx");
        assertThat(DynamicIncludeProcessor.replacePlaceholders("{foo}", file, attributeGetter)).isEqualTo("bar");
        assertThat(DynamicIncludeProcessor.replacePlaceholders("xxx{foo}", file, attributeGetter)).isEqualTo("xxxbar");
        assertThat(DynamicIncludeProcessor.replacePlaceholders("{foo}xxx", file, attributeGetter)).isEqualTo("barxxx");
        assertThat(DynamicIncludeProcessor.replacePlaceholders("xxx{foo}xxx", file, attributeGetter)).isEqualTo("xxxbarxxx");

        assertThat(DynamicIncludeProcessor.replacePlaceholders("{baz}", file, attributeGetter)).isEqualTo("{baz}");
        assertThat(DynamicIncludeProcessor.replacePlaceholders("xxx{baz}", file, attributeGetter)).isEqualTo("xxx{baz}");
        assertThat(DynamicIncludeProcessor.replacePlaceholders("{baz}xxx", file, attributeGetter)).isEqualTo("{baz}xxx");
        assertThat(DynamicIncludeProcessor.replacePlaceholders("xxx{baz}xxx", file, attributeGetter)).isEqualTo("xxx{baz}xxx");

        assertThat(DynamicIncludeProcessor.replacePlaceholders("vscode://file{file-absolute-with-leading-slash}", file, attributeGetter)).isEqualTo("vscode://file/some/path/file.txt");
        assertThat(DynamicIncludeProcessor.replacePlaceholders("vscode://file{file-absolute-with-leading-slash}", file2, attributeGetter)).isEqualTo("vscode://file/C:/some/path/file.txt");

        assertThat(DynamicIncludeProcessor.replacePlaceholders("https://example.com/{file-relative-to-git-repository}", file, attributeGetter)).isEqualTo("https://example.com/file.txt");
        assertThat(DynamicIncludeProcessor.replacePlaceholders("https://example.com/{file-relative-to-gradle-projectdir}", file, attributeGetter)).isEqualTo("https://example.com/file.txt");
        assertThat(DynamicIncludeProcessor.replacePlaceholders("https://example.com/{file-relative-to-gradle-rootdir}", file, attributeGetter)).isEqualTo("https://example.com/path/file.txt");
    }
}
