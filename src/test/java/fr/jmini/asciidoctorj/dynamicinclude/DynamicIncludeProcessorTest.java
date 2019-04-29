package fr.jmini.asciidoctorj.dynamicinclude;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import fr.jmini.asciidoctorj.dynamicinclude.XrefHolder.XrefHolderType;

public class DynamicIncludeProcessorTest {

    @Test
    void testFindFiles() throws Exception {
        Path example1 = Paths.get("src/test/resources/example1");

        List<String> list1 = findFiles(example1, "pages/*.adoc", null, null);
        assertThat(list1).containsExactly("pages/page1.adoc", "pages/page2.adoc", "pages/zpage.adoc");

        List<String> list2 = findFiles(example1, "**/*.adoc", null, null);
        assertThat(list2).containsExactly("pages/page1.adoc", "pages/page2.adoc", "pages/zpage.adoc");

        List<String> list3 = findFiles(example1.resolve("pages"), "*.adoc", null, null);
        assertThat(list3).containsExactly("page1.adoc", "page2.adoc", "zpage.adoc");

        List<String> list4 = findFiles(example1, "pages/page*.adoc", null, null);
        assertThat(list4).containsExactly("pages/page1.adoc", "pages/page2.adoc");

        List<String> list5 = findFiles(example1, "**/*.adoc", "pages", null);
        assertThat(list5).containsExactly("pages/page1.adoc", "pages/page2.adoc", "pages/zpage.adoc");

        List<String> list6 = findFiles(example1, "**/*.adoc", "xxx", null);
        assertThat(list6).isEmpty();

        List<String> list7 = findFiles(example1.resolve("pages"), "*.adoc", "xxx", null);
        assertThat(list7).isEmpty();

        Path example4 = Paths.get("src/test/resources/example4");

        List<String> list8 = findFiles(example4, "**/*.adoc", "scope1", null);
        assertThat(list8).containsExactly(
                "scope1/areaA/ipsum.adoc",
                "scope1/areaA/lorem.adoc",
                "scope1/areaB/main.adoc",
                "scope1/areaB/sub1/sub1.adoc",
                "scope1/areaB/sub2/sub2b.adoc");

        List<String> list9 = findFiles(example4, "**/*.adoc", "scope1:scope2", null);
        assertThat(list9).containsExactly(
                "scope1/areaA/ipsum.adoc",
                "scope1/areaA/lorem.adoc",
                "scope1/areaB/main.adoc",
                "scope1/areaB/sub1/sub1.adoc",
                "scope1/areaB/sub2/sub2b.adoc",
                "scope2/areaA/ipsum.adoc",
                "scope2/areaA/lorem.adoc",
                "scope2/areaC/areaC.adoc");

        List<String> list10 = findFiles(example4, "**/*.adoc", "scope1:scope2:xxx", null);
        assertThat(list10).containsExactly(
                "scope1/areaA/ipsum.adoc",
                "scope1/areaA/lorem.adoc",
                "scope1/areaB/main.adoc",
                "scope1/areaB/sub1/sub1.adoc",
                "scope1/areaB/sub2/sub2b.adoc",
                "scope2/areaA/ipsum.adoc",
                "scope2/areaA/lorem.adoc",
                "scope2/areaC/areaC.adoc");

        List<String> list11 = findFiles(example4, "**/*.adoc", "scope1:scope2", "xxx");
        assertThat(list11).isEmpty();

        List<String> list12 = findFiles(example4, "**/*.adoc", "scope1:scope2", "areaA");
        assertThat(list12).containsExactly(
                "scope1/areaA/ipsum.adoc",
                "scope1/areaA/lorem.adoc",
                "scope2/areaA/ipsum.adoc",
                "scope2/areaA/lorem.adoc");

        List<String> list13 = findFiles(example4, "**/*.adoc", "scope1:scope2:xxx", "areaA:areaB");
        assertThat(list13).containsExactly(
                "scope1/areaA/ipsum.adoc",
                "scope1/areaA/lorem.adoc",
                "scope1/areaB/main.adoc",
                "scope1/areaB/sub1/sub1.adoc",
                "scope1/areaB/sub2/sub2b.adoc",
                "scope2/areaA/ipsum.adoc",
                "scope2/areaA/lorem.adoc");

        List<String> list14 = findFiles(example4, "**/*.adoc", "scope1:scope2:xxx", "areaA:areaB:xxx");
        assertThat(list14).containsExactly(
                "scope1/areaA/ipsum.adoc",
                "scope1/areaA/lorem.adoc",
                "scope1/areaB/main.adoc",
                "scope1/areaB/sub1/sub1.adoc",
                "scope1/areaB/sub2/sub2b.adoc",
                "scope2/areaA/ipsum.adoc",
                "scope2/areaA/lorem.adoc");
    }

    private List<String> findFiles(Path dir, String glob, String scopesValue, String areasValue) throws IOException {
        List<String> list = DynamicIncludeProcessor.findFiles(dir, glob, scopesValue, areasValue)
                .stream()
                .map(p -> dir.relativize(p)
                        .toString())
                .sorted()
                .collect(Collectors.toList());
        return list;
    }

    @Test
    void testSortList() throws Exception {
        List<Integer> input = Arrays.asList(10, 24, 52, 3, 43, 91);

        List<String> order1 = Collections.emptyList();
        List<Integer> list1 = DynamicIncludeProcessor.sortList(input, order1, i -> i.toString());
        assertThat(list1).containsExactly(10, 24, 3, 43, 52, 91);

        List<String> order2 = Arrays.asList("3", "10", "91", "52", "43", "24");
        List<Integer> list2 = DynamicIncludeProcessor.sortList(input, order2, i -> i.toString());
        assertThat(list2).containsExactly(3, 10, 91, 52, 43, 24);

        List<String> order3 = Arrays.asList("91", "3");
        List<Integer> list3 = DynamicIncludeProcessor.sortList(input, order3, i -> i.toString());
        assertThat(list3).containsExactly(91, 3, 10, 24, 43, 52);
    }

    @Test
    void testReplaceXrefDoubleAngledBracketLinks() throws Exception {
        Path dir = Paths.get("dir");
        Path page1 = dir.resolve("folder/page.adoc");
        Path page2 = dir.resolve("folder/other.adoc");
        FileHolder holder1 = new FileHolder(page1, "folder/page.adoc", "!! dummy content !!", TitleType.PRESENT, "Page 1", 2, "_page_1", 91, 95);
        FileHolder holder2 = new FileHolder(page2, "folder/other.adoc", "!! dummy content !!", TitleType.PRESENT, "Other Page", 2, "_other_page", 101, 105);
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
        FileHolder holder1 = new FileHolder(page1, "folder/page.adoc", "!! dummy content !!", TitleType.PRESENT, "Page 1", 2, "_page_1", 91, 95);
        FileHolder holder2 = new FileHolder(page2, "folder/other.adoc", "!! dummy content !!", TitleType.PRESENT, "Other Page", 2, "_other_page", 101, 105);
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
        FileHolder holder1 = DynamicIncludeProcessor.createFileHolder(dir, path1, "_", "_");
        assertThat(holder1.getKey()).isEqualTo("example2/content/content1.adoc");
        assertThat(holder1.getTitleType()).isEqualTo(TitleType.PRESENT);
        assertThat(holder1.getTitle()).isEqualTo("Content 1");
        assertThat(holder1.getTitleEnd()).isGreaterThan(90);

        Path path2 = dir.resolve("example1/pages/page2.adoc");
        FileHolder holder2 = DynamicIncludeProcessor.createFileHolder(dir, path2, "_", "_");
        assertThat(holder2.getKey()).isEqualTo("example1/pages/page2.adoc");
        assertThat(holder2.getTitleType()).isEqualTo(TitleType.ABSENT);
        assertThat(holder2.getTitleEnd()).isEqualTo(0);

        Path path3 = dir.resolve("page.adoc");
        FileHolder holder3 = DynamicIncludeProcessor.createFileHolder(dir, path3, "_", "_");
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
}
