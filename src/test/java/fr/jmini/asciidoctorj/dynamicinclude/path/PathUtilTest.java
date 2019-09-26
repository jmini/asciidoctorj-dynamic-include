package fr.jmini.asciidoctorj.dynamicinclude.path;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

public class PathUtilTest {

    @Test
    void testGetNameSuffix() throws Exception {
        assertThat(PathUtil.getNameSuffix("test.adoc")).isNull();
        assertThat(PathUtil.getNameSuffix("test")).isNull();
        assertThat(PathUtil.getNameSuffix(".adoc")).isNull();
        assertThat(PathUtil.getNameSuffix("test.internal.adoc")).isEqualTo("internal");
        assertThat(PathUtil.getNameSuffix("test..adoc")).isEqualTo("");
        assertThat(PathUtil.getNameSuffix("..adoc")).isEqualTo("");
    }

    @Test
    void testNameWithoutSuffix() throws Exception {
        assertThat(PathUtil.getNameWithoutSuffix("test.adoc")).isEqualTo("test");
        assertThat(PathUtil.getNameWithoutSuffix("test")).isEqualTo("test");
        assertThat(PathUtil.getNameWithoutSuffix(".adoc")).isEqualTo("");
        assertThat(PathUtil.getNameWithoutSuffix("test.internal.adoc")).isEqualTo("test");
        assertThat(PathUtil.getNameWithoutSuffix("test..adoc")).isEqualTo("test");
        assertThat(PathUtil.getNameWithoutSuffix("..adoc")).isEqualTo("");
    }

    @Test
    void testGetCommonPath() throws Exception {
        assertThat(PathUtil.getCommonPath(Paths.get("/abc/xxx/file.txt"), Paths.get("/abc/xxx/other.txt"))).isEqualByComparingTo(Paths.get("/abc/xxx"));
        assertThat(PathUtil.getCommonPath(Paths.get("/abc/xxx/file.txt"), Paths.get("/abc/yyy/file.txt"))).isEqualByComparingTo(Paths.get("/abc"));
        assertThat(PathUtil.getCommonPath(Paths.get("/abc/xxx/file.txt"), Paths.get("/abc/file.txt"))).isEqualByComparingTo(Paths.get("/abc"));
        assertThat(PathUtil.getCommonPath(Paths.get("/abc/xxx/file.txt"), Paths.get("/xyz/file.txt"))).isEqualByComparingTo(Paths.get("/"));
        assertThat(PathUtil.getCommonPath(Paths.get("/abc/xxx/file.txt"), Paths.get("/"))).isEqualByComparingTo(Paths.get("/"));
        assertThat(PathUtil.getCommonPath(Paths.get("/"), Paths.get("/abc/xxx/file.txt"))).isEqualByComparingTo(Paths.get("/"));
        assertThat(PathUtil.getCommonPath(Paths.get("C:/Test/This"), Paths.get("C:/Test/That"))).isEqualByComparingTo(Paths.get("C:/Test"));
    }

    @Test
    void testCompare() throws Exception {
        List<String> list1 = Arrays.asList(
                "/folder/file.adoc",
                "/folder/xxx/file2.adoc",
                "/folder/xxx/page1.adoc");
        runCompare(p -> null, list1, list1, Collections.emptyList());

        List<String> list2 = Arrays.asList(
                "/folder/xxx/page1.adoc",
                "/folder/xxx/file2.adoc",
                "/folder/file.adoc");
        runCompare(p -> null, list2, list1, Collections.emptyList());

        List<String> list3 = Arrays.asList(
                "/folder/file.adoc",
                "/folder/xxx/index.adoc",
                "/folder/xxx/alpha.adoc");
        runCompare(p -> null, list3, list3, Collections.emptyList());

        List<String> list4 = Arrays.asList(
                "/folder/file.adoc",
                "/folder/file.internal.adoc",
                "/folder/file.private.adoc");
        runCompare(p -> null, list4, list4, Collections.emptyList());

        List<String> list5 = Arrays.asList(
                "/folder/file.private.adoc",
                "/folder/file.adoc",
                "/folder/file.internal.adoc");
        runCompare(p -> null, list5, list4, Collections.emptyList());

        List<String> list6 = Arrays.asList(
                "/folder/index.adoc",
                "/folder/content1.adoc",
                "/folder/content2.adoc");
        runCompare(p -> null, list6, list6, Collections.emptyList());

        List<String> list7 = Arrays.asList(
                "/folder/content2.adoc",
                "/folder/content1.adoc",
                "/folder/index.adoc");
        runCompare(p -> null, list7, list6, Collections.emptyList());
    }

    @Test
    void testCompareWithOrder() throws Exception {
        List<String> expectedMessages = Arrays.asList(
                "No ordering indication for 'test' in '/folder', putting it at the end",
                "No ordering indication for 'file' in '/folder', putting it at the end");
        List<String> list1 = Arrays.asList(
                "/folder/file.adoc",
                "/folder/test/lorem.adoc",
                "/folder/test/ipsum.adoc",
                "/folder/test/dolor.adoc");
        runCompare(p -> Arrays.asList("lorem", "ipsum", "dolor"), list1, list1, expectedMessages);

        List<String> list2 = Arrays.asList(
                "/folder/test/ipsum.adoc",
                "/folder/test/lorem.adoc",
                "/folder/file.adoc",
                "/folder/test/dolor.adoc");
        runCompare(p -> Arrays.asList("lorem", "ipsum", "dolor"), list2, list1, expectedMessages);

        List<String> list3 = Arrays.asList(
                "/folder/file.adoc",
                "/folder/test/index.adoc",
                "/folder/test/lorem.adoc",
                "/folder/test/dolor.adoc");
        runCompare(p -> Arrays.asList("lorem", "ipsum", "dolor"), list3, list3, expectedMessages);

        List<String> list4 = Arrays.asList(
                "/folder/index.adoc",
                "/folder/index.internal.adoc",
                "/folder/index.private.adoc",
                "/folder/lorem.adoc",
                "/folder/lorem.internal.adoc",
                "/folder/lorem.private.adoc",
                "/folder/ipsum.adoc");
        runCompare(p -> Arrays.asList("lorem", "ipsum", "dolor"), list4, list4, expectedMessages);

        List<String> list5 = Arrays.asList(
                "/folder/index.internal.adoc",
                "/folder/lorem.internal.adoc",
                "/folder/index.adoc",
                "/folder/ipsum.adoc",
                "/folder/lorem.adoc",
                "/folder/index.private.adoc",
                "/folder/lorem.private.adoc");
        runCompare(p -> Arrays.asList("lorem", "ipsum", "dolor"), list5, list4, expectedMessages);
    }

    private void runCompare(Function<Path, List<String>> orderSupplier, List<String> list, List<String> expected, List<String> expectedMessages) {
        AbsolutePathComparator comparator = new AbsolutePathComparator(orderSupplier);
        List<String> result = list.stream()
                .map(Paths::get)
                .sorted(comparator)
                .map(Path::toString)
                .collect(Collectors.toList());
        assertThat(result).isEqualTo(expected);
        assertThat(comparator.getMessages()).isSubsetOf(expectedMessages);
    }

    @Test
    void testFindAndSortFiles() throws Exception {
        Path example1 = Paths.get("src/test/resources/example1")
                .toAbsolutePath();

        List<String> list101 = findAndSortFiles(example1, "pages/*.adoc", Collections.emptyList());
        assertThat(list101).containsExactly("pages/page1.adoc", "pages/page2.adoc", "pages/zpage.adoc");

        List<String> list102 = findAndSortFiles(example1, "**/*.adoc", Collections.emptyList());
        assertThat(list102).containsExactly("pages/page1.adoc", "pages/page2.adoc", "pages/zpage.adoc", "pub/pub.adoc");

        List<String> list103 = findAndSortFiles(example1.resolve("pages"), "*.adoc", Collections.emptyList());
        assertThat(list103).containsExactly("page1.adoc", "page2.adoc", "zpage.adoc");

        List<String> list104 = findAndSortFiles(example1, "pages/page*.adoc", Collections.emptyList());
        assertThat(list104).containsExactly("pages/page1.adoc", "pages/page2.adoc");

        Path example2 = Paths.get("src/test/resources/example2")
                .toAbsolutePath();

        List<String> list201 = findAndSortFiles(example2, "content/*.adoc", Collections.emptyList());
        assertThat(list201).containsExactly("content/index.adoc", "content/content1.adoc", "content/content2.adoc");

        Path example3 = Paths.get("src/test/resources/example3")
                .toAbsolutePath();

        List<String> list301 = findAndSortFiles(example3, "cnt/*.adoc", Collections.emptyList());
        assertThat(list301).containsExactly("cnt/index.adoc", "cnt/lorem.adoc", "cnt/ipsum.adoc", "cnt/dolor.adoc");
    }

    private List<String> findAndSortFiles(Path dir, String glob, List<String> nameSuffixes) throws IOException {
        List<String> messages = new ArrayList<>();
        List<Path> findFiles = PathUtil.findFiles(dir, glob, nameSuffixes);
        List<Path> sortedFiles = PathUtil.sortFiles(messages::add, findFiles);
        List<String> list = sortedFiles
                .stream()
                .map(p -> dir.relativize(p)
                        .toString()
                        .replace('\\', '/'))
                .collect(Collectors.toList());
        assertThat(messages).isEmpty();
        return list;
    }

}
