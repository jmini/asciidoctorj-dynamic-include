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
        Function<Path, List<String>> orderSupplier = p -> null;
        List<String> nameSuffixes = Collections.emptyList();

        List<String> list1 = Arrays.asList(
                "/folder/file.adoc",
                "/folder/xxx/file2.adoc",
                "/folder/xxx/page1.adoc");
        runCompare(orderSupplier, nameSuffixes, list1, list1, Collections.emptyList());

        List<String> list2 = Arrays.asList(
                "/folder/xxx/page1.adoc",
                "/folder/xxx/file2.adoc",
                "/folder/file.adoc");
        runCompare(orderSupplier, nameSuffixes, list2, list1, Collections.emptyList());

        List<String> list3 = Arrays.asList(
                "/folder/file.adoc",
                "/folder/xxx/index.adoc",
                "/folder/xxx/alpha.adoc");
        runCompare(orderSupplier, nameSuffixes, list3, list3, Collections.emptyList());

        List<String> list4 = Arrays.asList(
                "/folder/file.adoc",
                "/folder/file.internal.adoc",
                "/folder/file.private.adoc");
        runCompare(orderSupplier, nameSuffixes, list4, list4, Collections.emptyList());

        List<String> list5 = Arrays.asList(
                "/folder/file.private.adoc",
                "/folder/file.adoc",
                "/folder/file.internal.adoc");
        runCompare(orderSupplier, nameSuffixes, list5, list4, Collections.emptyList());

        List<String> list6 = Arrays.asList(
                "/folder/index.adoc",
                "/folder/content1.adoc",
                "/folder/content2.adoc");
        runCompare(orderSupplier, nameSuffixes, list6, list6, Collections.emptyList());

        List<String> list7 = Arrays.asList(
                "/folder/content2.adoc",
                "/folder/content1.adoc",
                "/folder/index.adoc");
        runCompare(orderSupplier, nameSuffixes, list7, list6, Collections.emptyList());
    }

    @Test
    void testCompareWithSuffix() throws Exception {
        Function<Path, List<String>> orderSupplier = p -> null;
        List<String> nameSuffixes = Arrays.asList("lorem", "ipsum", "dolor");

        List<String> list1 = Arrays.asList(
                "/folder/file.adoc",
                "/folder/file.lorem.adoc",
                "/folder/file.ipsum.adoc",
                "/folder/file.dolor.adoc");
        runCompare(orderSupplier, nameSuffixes, list1, list1, Collections.emptyList());

        List<String> list2 = Arrays.asList(
                "/folder/file.ipsum.adoc",
                "/folder/file.lorem.adoc",
                "/folder/file.adoc",
                "/folder/file.dolor.adoc");
        runCompare(orderSupplier, nameSuffixes, list2, list1, Collections.emptyList());

        List<String> list3 = Arrays.asList(
                "/folder/index.adoc",
                "/folder/index.ipsum.adoc",
                "/folder/file.adoc",
                "/folder/file.lorem.adoc",
                "/folder/file.ipsum.adoc");
        runCompare(orderSupplier, nameSuffixes, list3, list3, Collections.emptyList());

        List<String> list4 = Arrays.asList(
                "/folder/file.adoc",
                "/folder/file.lorem.adoc",
                "/folder/file.ipsum.adoc",
                "/folder/index.adoc",
                "/folder/index.ipsum.adoc");
        runCompare(orderSupplier, nameSuffixes, list4, list3, Collections.emptyList());
    }

    @Test
    void testCompareWithOrder() throws Exception {
        Function<Path, List<String>> orderSupplier = p -> Arrays.asList("lorem", "ipsum", "dolor");
        List<String> nameSuffixes = Collections.emptyList();

        List<String> expectedMessages = Arrays.asList(
                "No ordering indication for 'test' in '/folder', putting it at the end",
                "No ordering indication for 'file' in '/folder', putting it at the end");
        List<String> list1 = Arrays.asList(
                "/folder/file.adoc",
                "/folder/test/lorem.adoc",
                "/folder/test/ipsum.adoc",
                "/folder/test/dolor.adoc");
        runCompare(orderSupplier, nameSuffixes, list1, list1, expectedMessages);

        List<String> list2 = Arrays.asList(
                "/folder/test/ipsum.adoc",
                "/folder/test/lorem.adoc",
                "/folder/file.adoc",
                "/folder/test/dolor.adoc");
        runCompare(orderSupplier, nameSuffixes, list2, list1, expectedMessages);

        List<String> list3 = Arrays.asList(
                "/folder/file.adoc",
                "/folder/test/index.adoc",
                "/folder/test/lorem.adoc",
                "/folder/test/dolor.adoc");
        runCompare(orderSupplier, nameSuffixes, list3, list3, expectedMessages);

        List<String> list4 = Arrays.asList(
                "/folder/index.adoc",
                "/folder/index.internal.adoc",
                "/folder/index.private.adoc",
                "/folder/lorem.adoc",
                "/folder/lorem.internal.adoc",
                "/folder/lorem.private.adoc",
                "/folder/ipsum.adoc");
        runCompare(orderSupplier, nameSuffixes, list4, list4, expectedMessages);

        List<String> list5 = Arrays.asList(
                "/folder/index.internal.adoc",
                "/folder/lorem.internal.adoc",
                "/folder/index.adoc",
                "/folder/ipsum.adoc",
                "/folder/lorem.adoc",
                "/folder/index.private.adoc",
                "/folder/lorem.private.adoc");
        runCompare(orderSupplier, nameSuffixes, list5, list4, expectedMessages);
    }

    @Test
    void testCompareWithOrderAndSuffix() throws Exception {
        Function<Path, List<String>> orderSupplier = p -> Arrays.asList("lorem", "ipsum", "dolor");
        List<String> nameSuffixes = Arrays.asList("private", "internal");
        List<String> expectedMessages = Collections.emptyList();

        List<String> list1 = Arrays.asList(
                "/folder/lorem.adoc",
                "/folder/lorem.private.adoc",
                "/folder/lorem.internal.adoc",
                "/folder/ipsum.adoc",
                "/folder/ipsum.internal.adoc",
                "/folder/dolor.adoc",
                "/folder/dolor.private.adoc");
        runCompare(orderSupplier, nameSuffixes, list1, list1, expectedMessages);

        List<String> list2 = Arrays.asList(
                "/folder/dolor.adoc",
                "/folder/lorem.internal.adoc",
                "/folder/ipsum.internal.adoc",
                "/folder/ipsum.adoc",
                "/folder/lorem.private.adoc",
                "/folder/dolor.private.adoc",
                "/folder/lorem.adoc");
        runCompare(orderSupplier, nameSuffixes, list2, list1, expectedMessages);

        List<String> list3 = Arrays.asList(
                "/folder/index.adoc",
                "/folder/index.private.adoc",
                "/folder/index.internal.adoc",
                "/folder/lorem.adoc",
                "/folder/lorem.private.adoc",
                "/folder/lorem.internal.adoc",
                "/folder/ipsum.adoc");
        runCompare(orderSupplier, nameSuffixes, list3, list3, expectedMessages);

        List<String> list4 = Arrays.asList(
                "/folder/index.internal.adoc",
                "/folder/lorem.internal.adoc",
                "/folder/index.adoc",
                "/folder/ipsum.adoc",
                "/folder/lorem.adoc",
                "/folder/index.private.adoc",
                "/folder/lorem.private.adoc");
        runCompare(orderSupplier, nameSuffixes, list4, list3, expectedMessages);
    }

    private void runCompare(Function<Path, List<String>> orderSupplier, List<String> nameSuffixes, List<String> list, List<String> expected, List<String> expectedMessages) {
        AbsolutePathComparator comparator = new AbsolutePathComparator(orderSupplier, nameSuffixes);
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

        List<String> list101 = findAndSortFiles(example1, "index.adoc", "pages/*.adoc", Collections.emptyList());
        assertThat(list101).containsExactly("pages/index.adoc", "pages/page1.adoc", "pages/page2.adoc");

        List<String> list102 = findAndSortFiles(example1, "index.adoc", "**/*.adoc", Collections.emptyList());
        assertThat(list102).containsExactly("pages/index.adoc", "pages/page1.adoc", "pages/page2.adoc", "publish/publish.adoc", "publish/sub/main.adoc");

        List<String> list103 = findAndSortFiles(example1.resolve("pages"), "test.adoc", "*.adoc", Collections.emptyList());
        assertThat(list103).containsExactly("index.adoc", "page1.adoc", "page2.adoc");

        List<String> list104 = findAndSortFiles(example1, "index.adoc", "pages/page*.adoc", Collections.emptyList());
        assertThat(list104).containsExactly("pages/page1.adoc", "pages/page2.adoc");

        Path example2 = Paths.get("src/test/resources/example2")
                .toAbsolutePath();

        List<String> list201 = findAndSortFiles(example2, "index.adoc", "content/*.adoc", Collections.emptyList());
        assertThat(list201).containsExactly("content/index.adoc", "content/content1.adoc", "content/content2.adoc");

        Path example3 = Paths.get("src/test/resources/example3")
                .toAbsolutePath();

        List<String> list301 = findAndSortFiles(example3, "index.adoc", "cnt/*.adoc", Collections.emptyList());
        assertThat(list301).containsExactly("cnt/index.adoc", "cnt/lorem.adoc", "cnt/ipsum.adoc", "cnt/dolor.adoc");

        Path example4 = Paths.get("src/test/resources/example4")
                .toAbsolutePath();

        List<String> list401 = findAndSortFiles(example4, "index.adoc", "page*.adoc", Collections.emptyList());
        assertThat(list401).containsExactly("page-lorem.adoc", "page-ipsum.adoc", "page-dolor.adoc");

        List<String> list402 = findAndSortFiles(example4, "index.adoc", "*.adoc", Collections.emptyList());
        assertThat(list402).containsExactly("page-lorem.adoc", "page-ipsum.adoc", "page-dolor.adoc");

        List<String> list403 = findAndSortFiles(example4, "other.adoc", "*.adoc", Collections.emptyList());
        assertThat(list403).containsExactly("index.adoc", "page-lorem.adoc", "page-ipsum.adoc", "page-dolor.adoc");

        Path example6 = Paths.get("src/test/resources/example6")
                .toAbsolutePath();

        List<String> list601 = findAndSortFiles(example6, "index.adoc", "pages/*.adoc", Collections.emptyList());
        assertThat(list601).containsExactly("pages/index.adoc", "pages/page1.adoc", "pages/page2.adoc");

        List<String> list602 = findAndSortFiles(example6, "index.adoc", "pages/*.adoc", Collections.singletonList("internal"));
        assertThat(list602).containsExactly("pages/index.adoc", "pages/page1.adoc", "pages/page1.internal.adoc", "pages/page2.adoc", "pages/page2.internal.adoc");

        List<String> list603 = findAndSortFiles(example6, "index.adoc", "pages/*.adoc", Collections.singletonList("advanced"));
        assertThat(list603).containsExactly("pages/index.adoc", "pages/page1.adoc", "pages/page1.advanced.adoc", "pages/page2.adoc", "pages/page2.advanced.adoc");

        List<String> list604 = findAndSortFiles(example6, "index.adoc", "pages/*.adoc", Arrays.asList("advanced", "internal"));
        assertThat(list604).containsExactly("pages/index.adoc", "pages/page1.adoc", "pages/page1.advanced.adoc", "pages/page1.internal.adoc", "pages/page2.adoc", "pages/page2.advanced.adoc", "pages/page2.internal.adoc");

        List<String> list605 = findAndSortFiles(example6, "index.adoc", "pages/*.adoc", Arrays.asList("internal", "advanced"));
        assertThat(list605).containsExactly("pages/index.adoc", "pages/page1.adoc", "pages/page1.internal.adoc", "pages/page1.advanced.adoc", "pages/page2.adoc", "pages/page2.internal.adoc", "pages/page2.advanced.adoc");

    }

    private List<String> findAndSortFiles(Path dir, String currentFileName, String glob, List<String> nameSuffixes) throws IOException {
        List<String> messages = new ArrayList<>();
        List<Path> findFiles = PathUtil.findFiles(dir, glob, nameSuffixes);
        List<Path> filtered = PathUtil.filterCurrentFile(findFiles, dir.resolve(currentFileName));
        List<Path> sortedFiles = PathUtil.sortFiles(messages::add, filtered, nameSuffixes);
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
