package fr.jmini.asciidoctorj.dynamicinclude;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import fr.jmini.asciidoctorj.dynamicinclude.XrefHolder.XrefHolderType;

public class DynamicIncludeProcessorTest {

    @Test
    void testConvertLevelOffsetShifting() throws Exception {
        List<String> list;
        list = new ArrayList<String>();
        assertThat(DynamicIncludeProcessor.convertLevelOffsetShifting(list::add, "0")).isEqualTo(0);
        assertThat(list).isEmpty();

        list = new ArrayList<String>();
        assertThat(DynamicIncludeProcessor.convertLevelOffsetShifting(list::add, "1")).isEqualTo(1);
        assertThat(list).isEmpty();

        list = new ArrayList<String>();
        assertThat(DynamicIncludeProcessor.convertLevelOffsetShifting(list::add, "+1")).isEqualTo(1);
        assertThat(list).isEmpty();

        list = new ArrayList<String>();
        assertThat(DynamicIncludeProcessor.convertLevelOffsetShifting(list::add, "-1")).isEqualTo(-1);
        assertThat(list).isEmpty();

        list = new ArrayList<String>();
        assertThat(DynamicIncludeProcessor.convertLevelOffsetShifting(list::add, null)).isEqualTo(1);
        assertThat(list).isEmpty();

        list = new ArrayList<String>();
        assertThat(DynamicIncludeProcessor.convertLevelOffsetShifting(list::add, "abc")).isEqualTo(1);
        assertThat(list).containsOnly("level-offset-shifting value 'abc' is not a valid number, using 1 as fallback");

        list = new ArrayList<String>();
        assertThat(DynamicIncludeProcessor.convertLevelOffsetShifting(list::add, "")).isEqualTo(1);
        assertThat(list).containsOnly("level-offset-shifting value '' is not a valid number, using 1 as fallback");
    }

    @Test
    void testOutputOffset() throws Exception {
        assertThat(DynamicIncludeProcessor.outputOffset(0)).isEqualTo("0");
        assertThat(DynamicIncludeProcessor.outputOffset(1)).isEqualTo("+1");
        assertThat(DynamicIncludeProcessor.outputOffset(2)).isEqualTo("+2");
        assertThat(DynamicIncludeProcessor.outputOffset(-1)).isEqualTo("-1");
        assertThat(DynamicIncludeProcessor.outputOffset(-2)).isEqualTo("-2");
    }

    @Test
    void testCalculateOffset() throws Exception {
        Path dir = Paths.get("dir");

        Path page1 = dir.resolve("folder/index.adoc");
        assertThat(DynamicIncludeProcessor.calculateOffset(dir, page1, "index", 2, 1)).isEqualTo(0);
        assertThat(DynamicIncludeProcessor.calculateOffset(dir, page1, "index", 2, 2)).isEqualTo(1);

        Path page2 = dir.resolve("folder/page.adoc");
        assertThat(DynamicIncludeProcessor.calculateOffset(dir, page2, "page", 2, 1)).isEqualTo(1);
        assertThat(DynamicIncludeProcessor.calculateOffset(dir, page2, "page", 2, 2)).isEqualTo(2);
    }

    @Test
    void testReplaceXrefDoubleAngledBracketLinks() throws Exception {
        Path dir = Paths.get("dir");
        Path page1 = dir.resolve("folder/page.adoc");
        Path page2 = dir.resolve("folder/other.adoc");
        FileHolder holder1 = new FileHolder(page1, "folder/page.adoc", "page", null, "!! dummy content !!", TitleType.PRESENT, "Page 1", 2, 1, "_page_1", 91, 95);
        FileHolder holder2 = new FileHolder(page2, "folder/other.adoc", "other", null, "!! dummy content !!", TitleType.PRESENT, "Other Page", 2, 1, "_other_page", 101, 105);
        List<FileHolder> list = Arrays.asList(holder1, holder2);

        String emptyList = DynamicIncludeProcessor.replaceXrefDoubleAngledBracketLinks("Some content", Collections.emptyList(), dir, page1, dir, true);
        assertThat(emptyList).isEqualTo("Some content");

        String oneElement = DynamicIncludeProcessor.replaceXrefDoubleAngledBracketLinks("Some content", Collections.singletonList(holder1), dir, page1, dir, true);
        assertThat(oneElement).isEqualTo("Some content");

        String otherLinkStart = DynamicIncludeProcessor.replaceXrefDoubleAngledBracketLinks("<<other.adoc#test, other>> some link", list, dir, page1, dir, true);
        assertThat(otherLinkStart).isEqualTo("<<#test, other>> some link");

        String otherLink = DynamicIncludeProcessor.replaceXrefDoubleAngledBracketLinks("Some <<other.adoc#test, other>> link", list, dir, page1, dir, true);
        assertThat(otherLink).isEqualTo("Some <<#test, other>> link");

        String otherLinkEnd = DynamicIncludeProcessor.replaceXrefDoubleAngledBracketLinks("Some link <<other.adoc#test, other>>", list, dir, page1, dir, true);
        assertThat(otherLinkEnd).isEqualTo("Some link <<#test, other>>");

        String otherLinkNoText = DynamicIncludeProcessor.replaceXrefDoubleAngledBracketLinks("<<other.adoc#test>> some link", list, dir, page1, dir, true);
        assertThat(otherLinkNoText).isEqualTo("<<#test>> some link");

        String otherLinkMutiple = DynamicIncludeProcessor.replaceXrefDoubleAngledBracketLinks("See <<other.adoc#foo, link 1>> and <<other.adoc#bar, link 2>> for more info", list, dir, page1, dir, true);
        assertThat(otherLinkMutiple).isEqualTo("See <<#foo, link 1>> and <<#bar, link 2>> for more info");

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

        String rootLinkNoAnchorX = DynamicIncludeProcessor.replaceXrefDoubleAngledBracketLinks("Some <<{root}folder/other.adoc#>> link", list, dir, page1, dir, true);
        assertThat(rootLinkNoAnchorX).isEqualTo("Some <<#_other_page>> link");
    }

    @Test
    void testReplaceXrefInlineLinks() throws Exception {
        Path dir = Paths.get("dir");
        Path page1 = dir.resolve("folder/page.adoc");
        Path page2 = dir.resolve("folder/other.adoc");
        FileHolder holder1 = new FileHolder(page1, "folder/page.adoc", null, null, "!! dummy content !!", TitleType.PRESENT, "Page 1", 2, 1, "_page_1", 91, 95);
        FileHolder holder2 = new FileHolder(page2, "folder/other.adoc", null, null, "!! dummy content !!", TitleType.PRESENT, "Other Page", 2, 1, "_other_page", 101, 105);
        List<FileHolder> list = Arrays.asList(holder1, holder2);

        String emptyList = DynamicIncludeProcessor.replaceXrefInlineLinks("Some content", Collections.emptyList(), dir, page1, dir, true);
        assertThat(emptyList).isEqualTo("Some content");

        String oneElement = DynamicIncludeProcessor.replaceXrefInlineLinks("Some content", Collections.singletonList(holder1), dir, page1, dir, true);
        assertThat(oneElement).isEqualTo("Some content");

        String otherLinkStart = DynamicIncludeProcessor.replaceXrefInlineLinks("xref:other.adoc#test[other] some link", list, dir, page1, dir, true);
        assertThat(otherLinkStart).isEqualTo("xref:#test[other] some link");

        String otherLink = DynamicIncludeProcessor.replaceXrefInlineLinks("Some xref:other.adoc#test[other] link", list, dir, page1, dir, true);
        assertThat(otherLink).isEqualTo("Some xref:#test[other] link");

        String otherLinkEnd = DynamicIncludeProcessor.replaceXrefInlineLinks("Some link xref:other.adoc#test[other]", list, dir, page1, dir, true);
        assertThat(otherLinkEnd).isEqualTo("Some link xref:#test[other]");

        String otherLinkMutiple = DynamicIncludeProcessor.replaceXrefInlineLinks("See xref:other.adoc#foo[link 1] and xref:other.adoc#bar[link 2] for more info", list, dir, page1, dir, true);
        assertThat(otherLinkMutiple).isEqualTo("See xref:#foo[link 1] and xref:#bar[link 2] for more info");

        String otherNoHashLink = DynamicIncludeProcessor.replaceXrefInlineLinks("Some xref:other.adoc[valuable] link", list, dir, page1, dir, true);
        assertThat(otherNoHashLink).isEqualTo("Some xref:#_other_page[valuable] link");

        String internalLink = DynamicIncludeProcessor.replaceXrefDoubleAngledBracketLinks("Some xref:#test[internal] link", list, dir, page1, dir, true);
        assertThat(internalLink).isEqualTo("Some xref:#test[internal] link");

        String externalLink = DynamicIncludeProcessor.replaceXrefInlineLinks("Some xref:ext.adoc#[other] link", list, dir, page1, dir, false);
        assertThat(externalLink).isEqualTo("Some xref:folder/ext.adoc#[other] link");

        String externalNoHashLink = DynamicIncludeProcessor.replaceXrefInlineLinks("Some xref:ext.adoc[other] link", list, dir, page1, dir, false);
        assertThat(externalNoHashLink).isEqualTo("Some xref:folder/ext.adoc[other] link");

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

        Optional<XrefHolder> noAnchorOpt = DynamicIncludeProcessor.findNextXrefInline("See xref:other.adoc[this link here] for more info", 0);
        assertThat(noAnchorOpt).isPresent();
        XrefHolder noAnchor = noAnchorOpt.get();
        assertThat(noAnchor.getType()).isEqualTo(XrefHolderType.INLINE);
        assertThat(noAnchor.getStartIndex()).isEqualTo(4);
        assertThat(noAnchor.getEndIndex()).isEqualTo(35);
        assertThat(noAnchor.getFile()).isEqualTo("other.adoc");
        assertThat(noAnchor.getAnchor()).isNull();
        assertThat(noAnchor.getText()).isEqualTo("this link here");
    }

    @Test
    void testHolderToAsciiDoc() throws Exception {
        // DoubleAngledBracket:
        runParseHolderToAsciiDoc("<<other.adoc#test, other>>", DynamicIncludeProcessor::findNextXrefDoubleAngledBracket);
        runParseHolderToAsciiDoc("<<test,internal>>", DynamicIncludeProcessor::findNextXrefDoubleAngledBracket);
        runParseHolderToAsciiDoc("<<here>>", DynamicIncludeProcessor::findNextXrefDoubleAngledBracket);
        runParseHolderToAsciiDoc("<<other.adoc#test>>", DynamicIncludeProcessor::findNextXrefDoubleAngledBracket);
        runParseHolderToAsciiDoc("<<other.adoc#>>", DynamicIncludeProcessor::findNextXrefDoubleAngledBracket);
        runParseHolderToAsciiDoc("<<other.adoc#, here>>", DynamicIncludeProcessor::findNextXrefDoubleAngledBracket);

        // Inline:
        runParseHolderToAsciiDoc("xref:other.adoc#test[other]", DynamicIncludeProcessor::findNextXrefInline);
        runParseHolderToAsciiDoc("xref:test[internal]", DynamicIncludeProcessor::findNextXrefInline);
        runParseHolderToAsciiDoc("xref:here[]", DynamicIncludeProcessor::findNextXrefInline);
        runParseHolderToAsciiDoc("xref:other.adoc#test[]", DynamicIncludeProcessor::findNextXrefInline);
        runParseHolderToAsciiDoc("xref:other.adoc#[]", DynamicIncludeProcessor::findNextXrefInline);
        runParseHolderToAsciiDoc("xref:other.adoc#[here]", DynamicIncludeProcessor::findNextXrefInline);
        runParseHolderToAsciiDoc("xref:other.adoc[]", DynamicIncludeProcessor::findNextXrefInline);
        runParseHolderToAsciiDoc("xref:other.adoc[test]", DynamicIncludeProcessor::findNextXrefInline);

        // Text:
        XrefHolder holder = new XrefHolder("file", "anchor", "text", XrefHolderType.TEXT, -1, -1);
        String string = DynamicIncludeProcessor.holderToAsciiDoc(holder);
        assertThat(string).isEqualTo("text");
    }

    private void runParseHolderToAsciiDoc(String input, BiFunction<String, Integer, Optional<XrefHolder>> function) {
        Optional<XrefHolder> findHolder = function.apply(input, 0);
        assertThat(findHolder).isPresent();
        XrefHolder holder = findHolder.get();
        String string = DynamicIncludeProcessor.holderToAsciiDoc(holder);
        assertThat(string).isEqualTo(input);
    }

    @Test
    void testCreateFileHolder() throws Exception {
        Path dir = Paths.get("src/test/resources")
                .toAbsolutePath();

        Path path1 = dir.resolve("example4/page-lorem.adoc");
        FileHolder holder1 = DynamicIncludeProcessor.createFileHolder(dir, path1, "_", "_", 1);
        assertThat(holder1.getKey()).isEqualTo("example4/page-lorem.adoc");
        assertThat(holder1.getTitleType()).isEqualTo(TitleType.ABSENT);
        assertThat(holder1.getTitleEnd()).isEqualTo(0);

        Path path2 = dir.resolve("example4/page-ipsum.adoc");
        FileHolder holder2 = DynamicIncludeProcessor.createFileHolder(dir, path2, "_", "_", 1);
        assertThat(holder2.getKey()).isEqualTo("example4/page-ipsum.adoc");
        assertThat(holder2.getTitleType()).isEqualTo(TitleType.COMMENTED);
        assertThat(holder2.getTitle()).isEqualTo("Ipsum");
        assertThat(holder2.getTitleEnd()).isEqualTo(10);

        Path path3 = dir.resolve("example4/page-dolor.adoc");
        FileHolder holder3 = DynamicIncludeProcessor.createFileHolder(dir, path3, "_", "_", 1);
        assertThat(holder3.getKey()).isEqualTo("example4/page-dolor.adoc");
        assertThat(holder3.getTitleType()).isEqualTo(TitleType.PRESENT);
        assertThat(holder3.getTitle()).isEqualTo("Dolor");
        assertThat(holder3.getTitleEnd()).isEqualTo(8);
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

    private FileHolder createFileHolder(Path dir, String subPath) {
        Path page = dir.resolve(subPath);
        return new FileHolder(page, subPath, null, null, "!! some content !!", TitleType.ABSENT, null, 0, 0, null, 100, 101);
    }
}
