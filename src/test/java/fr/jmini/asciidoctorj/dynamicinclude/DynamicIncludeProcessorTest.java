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

}
