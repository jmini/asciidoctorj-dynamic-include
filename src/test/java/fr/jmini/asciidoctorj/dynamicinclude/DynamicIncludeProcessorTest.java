package fr.jmini.asciidoctorj.dynamicinclude;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
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

        String emptyList = DynamicIncludeProcessor.replaceXrefDoubleAngledBracketLinks("Some content", Collections.emptyList(), dir, page1, dir);
        assertThat(emptyList).isEqualTo("Some content");

        String oneElement = DynamicIncludeProcessor.replaceXrefDoubleAngledBracketLinks("Some content", Collections.singletonList(page1), dir, page1, dir);
        assertThat(oneElement).isEqualTo("Some content");

        String page2LinkStart = DynamicIncludeProcessor.replaceXrefDoubleAngledBracketLinks("<<other.adoc#test, other>> some link", Arrays.asList(page1, page2), dir, page1, dir);
        assertThat(page2LinkStart).isEqualTo("<<#test, other>> some link");

        String page2Link = DynamicIncludeProcessor.replaceXrefDoubleAngledBracketLinks("Some <<other.adoc#test, other>> link", Arrays.asList(page1, page2), dir, page1, dir);
        assertThat(page2Link).isEqualTo("Some <<#test, other>> link");

        String page2LinkEnd = DynamicIncludeProcessor.replaceXrefDoubleAngledBracketLinks("Some link <<other.adoc#test, other>>", Arrays.asList(page1, page2), dir, page1, dir);
        assertThat(page2LinkEnd).isEqualTo("Some link <<#test, other>>");

        String internalLink = DynamicIncludeProcessor.replaceXrefDoubleAngledBracketLinks("Some <<test, internal>> link", Arrays.asList(page1, page2), dir, page1, dir);
        assertThat(internalLink).isEqualTo("Some <<test, internal>> link");

        String externalLink = DynamicIncludeProcessor.replaceXrefDoubleAngledBracketLinks("Some <<ext.adoc#test, other>> link", Arrays.asList(page1, page2), dir, page1, dir);
        assertThat(externalLink).isEqualTo("Some <<folder/ext.adoc#test, other>> link");

        String rootLink = DynamicIncludeProcessor.replaceXrefDoubleAngledBracketLinks("Some <<{root}folder/other.adoc#test, root>> link", Arrays.asList(page1, page2), dir, page1, dir);
        assertThat(rootLink).isEqualTo("Some <<#test, root>> link");

    }

    @Test
    void testReplaceXrefInlineLinks() throws Exception {
        String emptyList = DynamicIncludeProcessor.replaceXrefInlineLinks("Some content", Collections.emptyList());
        assertThat(emptyList).isEqualTo("Some content");

        String oneElement = DynamicIncludeProcessor.replaceXrefInlineLinks("Some content", Collections.singletonList(Paths.get("page.adoc")));
        assertThat(oneElement).isEqualTo("Some content");

    }
}
