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

public class DynamicIncludeProcessorTest {

    @Test
    void testFindFiles() throws Exception {
        Path dir = Paths.get("src/test/resources/example1");

        List<String> list1 = findFiles(dir, "pages/*.adoc");
        assertThat(list1).containsExactly("pages/page1.adoc", "pages/page2.adoc", "pages/zpage.adoc");

        List<String> list2 = findFiles(dir, "**/*.adoc");
        assertThat(list2).containsExactly("pages/page1.adoc", "pages/page2.adoc", "pages/zpage.adoc");

        List<String> list3 = findFiles(dir.resolve("pages"), "*.adoc");
        assertThat(list3).containsExactly("page1.adoc", "page2.adoc", "zpage.adoc");

        List<String> list4 = findFiles(dir, "pages/page*.adoc");
        assertThat(list4).containsExactly("pages/page1.adoc", "pages/page2.adoc");
    }

    private List<String> findFiles(Path dir, String glob) throws IOException {
        List<String> list = DynamicIncludeProcessor.findFiles(dir, glob)
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

        String emptyList = DynamicIncludeProcessor.replaceXrefDoubleAngledBracketLinks("Some content", Collections.emptyList(), dir, page1, dir);
        assertThat(emptyList).isEqualTo("Some content");

        String oneElement = DynamicIncludeProcessor.replaceXrefDoubleAngledBracketLinks("Some content", Collections.singletonList(holder1), dir, page1, dir);
        assertThat(oneElement).isEqualTo("Some content");

        String page2LinkStart = DynamicIncludeProcessor.replaceXrefDoubleAngledBracketLinks("<<other.adoc#test, other>> some link", list, dir, page1, dir);
        assertThat(page2LinkStart).isEqualTo("<<#test, other>> some link");

        String page2Link = DynamicIncludeProcessor.replaceXrefDoubleAngledBracketLinks("Some <<other.adoc#test, other>> link", list, dir, page1, dir);
        assertThat(page2Link).isEqualTo("Some <<#test, other>> link");

        String page2LinkEnd = DynamicIncludeProcessor.replaceXrefDoubleAngledBracketLinks("Some link <<other.adoc#test, other>>", list, dir, page1, dir);
        assertThat(page2LinkEnd).isEqualTo("Some link <<#test, other>>");

        String internalLink = DynamicIncludeProcessor.replaceXrefDoubleAngledBracketLinks("Some <<test, internal>> link", list, dir, page1, dir);
        assertThat(internalLink).isEqualTo("Some <<test, internal>> link");

        String externalLink = DynamicIncludeProcessor.replaceXrefDoubleAngledBracketLinks("Some <<ext.adoc#test, other>> link", list, dir, page1, dir);
        assertThat(externalLink).isEqualTo("Some <<folder/ext.adoc#test, other>> link");

        String rootLink = DynamicIncludeProcessor.replaceXrefDoubleAngledBracketLinks("Some <<{root}folder/other.adoc#test, root>> link", list, dir, page1, dir);
        assertThat(rootLink).isEqualTo("Some <<#test, root>> link");

        String root2Link = DynamicIncludeProcessor.replaceXrefDoubleAngledBracketLinks("Some <<{root}folder/other.adoc#test, root>> link", list, dir, page1, dir.resolve("folder/.."));
        assertThat(root2Link).isEqualTo("Some <<#test, root>> link");

        String rootLinkNoAnchor = DynamicIncludeProcessor.replaceXrefDoubleAngledBracketLinks("Some <<{root}folder/other.adoc#, root>> link", list, dir, page1, dir);
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

        String emptyList = DynamicIncludeProcessor.replaceXrefInlineLinks("Some content", Collections.emptyList(), dir, page1, dir);
        assertThat(emptyList).isEqualTo("Some content");

        String oneElement = DynamicIncludeProcessor.replaceXrefInlineLinks("Some content", Collections.singletonList(holder1), dir, page1, dir);
        assertThat(oneElement).isEqualTo("Some content");

        String page2LinkStart = DynamicIncludeProcessor.replaceXrefInlineLinks("xref:other.adoc#test[other] some link", list, dir, page1, dir);
        assertThat(page2LinkStart).isEqualTo("xref:#test[other] some link");

        String page2Link = DynamicIncludeProcessor.replaceXrefInlineLinks("Some xref:other.adoc#test[other] link", list, dir, page1, dir);
        assertThat(page2Link).isEqualTo("Some xref:#test[other] link");

        String page2LinkEnd = DynamicIncludeProcessor.replaceXrefInlineLinks("Some link xref:other.adoc#test[other]", list, dir, page1, dir);
        assertThat(page2LinkEnd).isEqualTo("Some link xref:#test[other]");

        String internalLink = DynamicIncludeProcessor.replaceXrefDoubleAngledBracketLinks("Some xref:#test[internal] link", list, dir, page1, dir);
        assertThat(internalLink).isEqualTo("Some xref:#test[internal] link");

        String externalLink = DynamicIncludeProcessor.replaceXrefInlineLinks("Some xref:ext.adoc[other] link", list, dir, page1, dir);
        assertThat(externalLink).isEqualTo("Some xref:folder/ext.adoc[other] link");

        String externalWithAnchorLink = DynamicIncludeProcessor.replaceXrefInlineLinks("Some xref:ext.adoc#test[other] link", list, dir, page1, dir);
        assertThat(externalWithAnchorLink).isEqualTo("Some xref:folder/ext.adoc#test[other] link");

        String rootLink = DynamicIncludeProcessor.replaceXrefInlineLinks("Some xref:{root}folder/other.adoc#test[root] link", list, dir, page1, dir);
        assertThat(rootLink).isEqualTo("Some xref:#test[root] link");

        String rootLinkNoAnchor = DynamicIncludeProcessor.replaceXrefInlineLinks("Some xref:{root}folder/other.adoc[root] link", list, dir, page1, dir);
        assertThat(rootLinkNoAnchor).isEqualTo("Some xref:#_other_page[root] link");

        String rootLinkEmptyAnchor = DynamicIncludeProcessor.replaceXrefInlineLinks("Some xref:{root}folder/other.adoc#[root] link", list, dir, page1, dir);
        assertThat(rootLinkEmptyAnchor).isEqualTo("Some xref:#_other_page[root] link");
    }

    @Test
    void testFindNextXrefDoubleAngledBracket() throws Exception {
        Optional<XrefHolder> emptyList = DynamicIncludeProcessor.findNextXrefDoubleAngledBracket("Some content", 0);
        assertThat(emptyList).isNotPresent();

        Optional<XrefHolder> linkStartOpt = DynamicIncludeProcessor.findNextXrefDoubleAngledBracket("<<other.adoc#test, other>> some link", 0);
        assertThat(linkStartOpt).isPresent();
        XrefHolder linkStart = linkStartOpt.get();
        assertThat(linkStart.isDoubleAngledBracketForm()).isTrue();
        assertThat(linkStart.getStartIndex()).isEqualTo(0);
        assertThat(linkStart.getEndIndex()).isEqualTo(26);
        assertThat(linkStart.getFile()).isEqualTo("other.adoc");
        assertThat(linkStart.getAnchor()).isEqualTo("test");
        assertThat(linkStart.getText()).isEqualTo(" other");

        Optional<XrefHolder> linkOpt = DynamicIncludeProcessor.findNextXrefDoubleAngledBracket("Some <<page1.adoc#sect, Other>> link", 0);
        assertThat(linkOpt).isPresent();
        XrefHolder link = linkOpt.get();
        assertThat(link.isDoubleAngledBracketForm()).isTrue();
        assertThat(link.getStartIndex()).isEqualTo(5);
        assertThat(link.getEndIndex()).isEqualTo(31);
        assertThat(link.getFile()).isEqualTo("page1.adoc");
        assertThat(link.getAnchor()).isEqualTo("sect");
        assertThat(link.getText()).isEqualTo(" Other");

        Optional<XrefHolder> linkEndOpt = DynamicIncludeProcessor.findNextXrefDoubleAngledBracket("Some link <<page2.adoc#here, OTHER>>", 0);
        assertThat(linkEndOpt).isPresent();
        XrefHolder linkEnd = linkEndOpt.get();
        assertThat(linkEnd.isDoubleAngledBracketForm()).isTrue();
        assertThat(linkEnd.getStartIndex()).isEqualTo(10);
        assertThat(linkEnd.getEndIndex()).isEqualTo(36);
        assertThat(linkEnd.getFile()).isEqualTo("page2.adoc");
        assertThat(linkEnd.getAnchor()).isEqualTo("here");
        assertThat(linkEnd.getText()).isEqualTo(" OTHER");

        Optional<XrefHolder> internalLinkOpt = DynamicIncludeProcessor.findNextXrefDoubleAngledBracket("Some <<test, internal>> link", 0);
        assertThat(internalLinkOpt).isPresent();
        XrefHolder internalLink = internalLinkOpt.get();
        assertThat(internalLink.isDoubleAngledBracketForm()).isTrue();
        assertThat(internalLink.getStartIndex()).isEqualTo(5);
        assertThat(internalLink.getEndIndex()).isEqualTo(23);
        assertThat(internalLink.getFile()).isNull();
        assertThat(internalLink.getAnchor()).isEqualTo("test");
        assertThat(internalLink.getText()).isEqualTo(" internal");

        Optional<XrefHolder> internalNoTextOpt = DynamicIncludeProcessor.findNextXrefDoubleAngledBracket("Some <<test>> link", 0);
        assertThat(internalNoTextOpt).isPresent();
        XrefHolder internalNoText = internalNoTextOpt.get();
        assertThat(internalNoText.isDoubleAngledBracketForm()).isTrue();
        assertThat(internalNoText.getStartIndex()).isEqualTo(5);
        assertThat(internalNoText.getEndIndex()).isEqualTo(13);
        assertThat(internalNoText.getFile()).isNull();
        assertThat(internalNoText.getAnchor()).isEqualTo("test");
        assertThat(internalNoText.getText()).isNull();

        Optional<XrefHolder> noTextOpt = DynamicIncludeProcessor.findNextXrefDoubleAngledBracket("Some <<other.adoc#test>> link", 0);
        assertThat(noTextOpt).isPresent();
        XrefHolder noText = noTextOpt.get();
        assertThat(noText.isDoubleAngledBracketForm()).isTrue();
        assertThat(noText.getStartIndex()).isEqualTo(5);
        assertThat(noText.getEndIndex()).isEqualTo(24);
        assertThat(noText.getFile()).isEqualTo("other.adoc");
        assertThat(noText.getAnchor()).isEqualTo("test");
        assertThat(noText.getText()).isNull();
    }

    @Test
    void testHolderToAsciiDoc() throws Exception {
        String input1 = "<<other.adoc#test, other>>";
        Optional<XrefHolder> findHolder1 = DynamicIncludeProcessor.findNextXrefDoubleAngledBracket(input1, 0);
        assertThat(findHolder1).isPresent();
        XrefHolder holder1 = findHolder1.get();
        String string1 = DynamicIncludeProcessor.holderToAsciiDoc(holder1);
        assertThat(string1).isEqualTo(input1);

        String input2 = "<<test,internal>>";
        Optional<XrefHolder> findHolder2 = DynamicIncludeProcessor.findNextXrefDoubleAngledBracket(input2, 0);
        assertThat(findHolder2).isPresent();
        XrefHolder holder2 = findHolder2.get();
        String string2 = DynamicIncludeProcessor.holderToAsciiDoc(holder2);
        assertThat(string2).isEqualTo(input2);

        String input3 = "<<here>>";
        Optional<XrefHolder> findHolder3 = DynamicIncludeProcessor.findNextXrefDoubleAngledBracket(input3, 0);
        assertThat(findHolder3).isPresent();
        XrefHolder holder3 = findHolder3.get();
        String string3 = DynamicIncludeProcessor.holderToAsciiDoc(holder3);
        assertThat(string3).isEqualTo(input3);

        String input4 = "<<other.adoc#test>>";
        Optional<XrefHolder> findHolder4 = DynamicIncludeProcessor.findNextXrefDoubleAngledBracket(input4, 0);
        assertThat(findHolder4).isPresent();
        XrefHolder holder4 = findHolder4.get();
        String string4 = DynamicIncludeProcessor.holderToAsciiDoc(holder4);
        assertThat(string4).isEqualTo(input4);
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
