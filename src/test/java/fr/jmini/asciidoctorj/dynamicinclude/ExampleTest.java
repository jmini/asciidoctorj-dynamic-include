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

public class ExampleTest {

    @Test
    public void testExample1Index() throws Exception {
        runTest("example1", "index");
    }

    @Test
    public void testExample1Index1() throws Exception {
        runTest("example1", "index1");
    }

    @Test
    public void testExample1Index2() throws Exception {
        runTest("example1", "index2");
    }

    @Test
    public void testExample2() throws Exception {
        List<LogRecord> logs = runTest("example2", "index");
        assertThat(logs).hasSize(1);
        LogRecord record = logs.get(0);
        assertThat(record.getMessage()).isEqualTo("list item index: expected 1, got 9");
        assertThat(record.getCursor()
                .getLineNumber()).isEqualTo(10);

    }

    private List<LogRecord> runTest(String folder, String fileName) throws IOException, URISyntaxException {
        Path exampleFolder = Paths.get("src/test/resources/" + folder)
                .toAbsolutePath();
        String content = new String(Files.readAllBytes(exampleFolder.resolve(fileName + ".adoc")), StandardCharsets.UTF_8);
        String expected = new String(Files.readAllBytes(exampleFolder.resolve(fileName + ".html")), StandardCharsets.UTF_8);

        Asciidoctor asciidoctor = Factory.create();
        InMemoryLogHanlder logHandler = new InMemoryLogHanlder();
        asciidoctor.registerLogHandler(logHandler);

        AttributesBuilder attributesBuilder = AttributesBuilder.attributes()
                .setAnchors(false)
                .sectionNumbers(false);

        OptionsBuilder optionsBuilder = OptionsBuilder.options()
                .attributes(attributesBuilder)
                .baseDir(exampleFolder.toFile())
                .option("sourcemap", true)
                .docType("article")
                .safe(SafeMode.UNSAFE);
        String html = asciidoctor.convert(content, optionsBuilder);

        assertThat(html).isEqualTo(expected);

        return logHandler.getLogs();
    }
}
