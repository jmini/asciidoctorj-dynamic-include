package fr.jmini.asciidoctorj.dynamicinclude;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.asciidoctor.Asciidoctor;
import org.asciidoctor.Asciidoctor.Factory;
import org.asciidoctor.AttributesBuilder;
import org.asciidoctor.OptionsBuilder;
import org.asciidoctor.SafeMode;
import org.asciidoctor.log.LogRecord;
import org.junit.jupiter.api.Test;

class ExampleTest {

    @Test
    void testExample1Index() throws Exception {
        Path logfile = Files.createTempFile("test", "log")
                .toAbsolutePath();

        List<LogRecord> logs = runTest("example1", "index", logfile.toString());
        assertThat(logs).isEmpty();

        Path expectedLogFile = Paths.get("src/test/resources/example1/index-log.txt");
        String expectedLogContent = new String(Files.readAllBytes(expectedLogFile), StandardCharsets.UTF_8);
        String content = readFile(logfile);
        Files.write(expectedLogFile, content.getBytes(StandardCharsets.UTF_8));
        assertThat(content).isEqualTo(expectedLogContent);
    }

    @Test
    void testExample1Guide() throws Exception {
        Path logfile = Files.createTempFile("test", "log")
                .toAbsolutePath();

        List<LogRecord> logs = runTest("example1", "guide", logfile.toString());
        assertThat(logs).isEmpty();

        String content = readFile(logfile);
        assertThat(content).isEqualTo("# File: \n" +
                "# Target: dynamic:pages/*.adoc\n" +
                "# level-offset-shifting: 2\n" +
                "pages/index.adoc (leveloffset: +1)\n" +
                "pages/page1.adoc (leveloffset: +2)\n" +
                "pages/page2.adoc (leveloffset: +2)\n\n");
    }

    @Test
    void testExample1OnlyPages() throws Exception {
        List<LogRecord> logs = runTest("example1", "only-pages", null);
        assertThat(logs).isEmpty();
    }

    @Test
    void testExample1Publish() throws Exception {
        Path logfile = Files.createTempFile("test", "log")
                .toAbsolutePath();

        List<LogRecord> logs = runTest("example1/publish", "publish", logfile.toString());
        assertThat(logs).isEmpty();

        String content = readFile(logfile);
        assertThat(content).isEqualTo("# File: \n" +
                "# Target: dynamic:../pages/*.adoc\n" +
                "# level-offset-shifting: 0\n" +
                "../pages/index.adoc (leveloffset: 0)\n" +
                "../pages/page1.adoc (leveloffset: +1)\n" +
                "../pages/page2.adoc (leveloffset: +1)\n\n");
    }

    @Test
    void testExample1SubPublish() throws Exception {
        Path logfile = Files.createTempFile("test", "log")
                .toAbsolutePath();

        List<LogRecord> logs = runTest("example1/publish/sub", "main", logfile.toString());
        assertThat(logs).isEmpty();

        String content = readFile(logfile);
        assertThat(content).isEqualTo("# File: \n" +
                "# Target: dynamic:../../pages/*.adoc\n" +
                "# level-offset-shifting: -1\n" +
                "../../pages/index.adoc (leveloffset: 0)\n" +
                "../../pages/page1.adoc (leveloffset: +1)\n" +
                "../../pages/page2.adoc (leveloffset: +1)\n\n");
    }

    @Test
    void testExample2() throws Exception {
        List<LogRecord> logs = runTest("example2", "index");
        assertThat(logs).hasSize(1);
        LogRecord record = logs.get(0);
        assertThat(record.getMessage()).isEqualTo("list item index: expected 1, got 9");
        assertThat(record.getCursor()
                .getLineNumber()).isEqualTo(10);
    }

    @Test
    void testExample3() throws Exception {
        List<LogRecord> logs = runTest("example3", "index");

        assertThat(logs).hasSize(2);

        LogRecord record1 = logs.get(0);
        assertThat(record1.getMessage()).isEqualTo("list item index: expected 1, got 7");
        assertThat(record1.getCursor()
                .getLineNumber()).isEqualTo(10);

        LogRecord record2 = logs.get(1);
        assertThat(record2.getMessage()).isEqualTo("list item index: expected 1, got 8");
        assertThat(record2.getCursor()
                .getLineNumber()).isEqualTo(10);
    }

    @Test
    void testExample4() throws Exception {
        List<LogRecord> logs = runTest("example4", "index");
        assertThat(logs).isEmpty();
    }

    @Test
    void testExample5() throws Exception {
        List<LogRecord> logs = runTest("example5", "index");
        assertThat(logs).hasSize(1);
        LogRecord record = logs.get(0);
        assertThat(record.getMessage()).startsWith("No ordering indication for 'info' in '");
        assertThat(record.getMessage()).endsWith("src/test/resources/example5/chapter-two', putting it at the end");
    }

    @Test
    void testExample6Simple() throws Exception {
        List<LogRecord> logs = runTest("example6", "simple-guide");
        assertThat(logs).isEmpty();
    }

    @Test
    void testExample6Advanced() throws Exception {
        List<LogRecord> logs = runTest("example6", "advanced-guide");
        assertThat(logs).isEmpty();
    }

    @Test
    void testExample6Internal() throws Exception {
        List<LogRecord> logs = runTest("example6", "internal-guide");
        assertThat(logs).isEmpty();
    }

    @Test
    void testExample6All() throws Exception {
        List<LogRecord> logs = runTest("example6", "all");
        assertThat(logs).isEmpty();
    }

    @Test
    void testExample7() throws Exception {
        List<LogRecord> logs = runTest("example7", "index", null, true);
        assertThat(logs).isEmpty();
    }

    @Test
    void testExample8() throws Exception {
        List<LogRecord> logs = runTest("example8", "index");
        assertThat(logs).isEmpty();
    }

    @Test
    void testExample9() throws Exception {
        List<LogRecord> logs = runTest("example9", "index");
        assertThat(logs).isEmpty();
    }

    @Test
    void testExample9XrefAsTest() throws Exception {
        List<LogRecord> logs = runTest("example9", "index2");
        assertThat(logs).isEmpty();
    }

    private List<LogRecord> runTest(String folder, String fileName) throws IOException, URISyntaxException {
        return runTest(folder, fileName, null, false);
    }

    private List<LogRecord> runTest(String folder, String fileName, String logfile) throws IOException, URISyntaxException {
        return runTest(folder, fileName, logfile, false);
    }

    private List<LogRecord> runTest(String folder, String fileName, String logfile, boolean asFile) throws IOException, URISyntaxException {
        Path exampleFolder = Paths.get("src/test/resources/" + folder)
                .toAbsolutePath();
        Path contentFile = exampleFolder.resolve(fileName + ".adoc");
        Path expectedFile = exampleFolder.resolve(fileName + ".html");
        String content = new String(Files.readAllBytes(contentFile), StandardCharsets.UTF_8);
        String expectedContent = new String(Files.readAllBytes(expectedFile), StandardCharsets.UTF_8);

        Asciidoctor asciidoctor = Factory.create();
        InMemoryLogHanlder logHandler = new InMemoryLogHanlder();
        asciidoctor.registerLogHandler(logHandler);

        AttributesBuilder attributesBuilder = AttributesBuilder.attributes()
                .setAnchors(false)
                .sectionNumbers(false)
                .attribute("nofooter", true);
        if (logfile != null) {
            attributesBuilder.attribute("dynamic-include-logfile", logfile);
        }
        attributesBuilder.attribute("local-git-repository-path", Paths.get("")
                .toAbsolutePath()
                .toString());

        OptionsBuilder optionsBuilder = OptionsBuilder.options()
                .attributes(attributesBuilder)
                .baseDir(exampleFolder.toFile())
                .docType("book")
                .safe(SafeMode.UNSAFE);
        String html, actual, expected;
        if (asFile) {
            asciidoctor.convertFile(contentFile.toFile(), optionsBuilder);
            html = new String(Files.readAllBytes(expectedFile), StandardCharsets.UTF_8);
            actual = contentAfterBody(html);
            expected = contentAfterBody(expectedContent);
        } else {
            html = asciidoctor.convert(content, optionsBuilder);
            actual = html;
            expected = expectedContent;
        }

        Files.write(expectedFile, html.getBytes(StandardCharsets.UTF_8));
        assertThat(actual).isEqualTo(expected);

        return logHandler.getLogs();
    }

    private String contentAfterBody(String html) {
        int i = html.indexOf("<body");
        if (i > 0) {
            return html.substring(i);
        }
        throw new IllegalStateException("Could not find '<body' in the content");
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
