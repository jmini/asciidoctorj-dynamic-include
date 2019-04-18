/*********************************************************************
* Copyright (c) 2019 Jeremie Bresson
*
* This program and the accompanying materials are made
* available under the terms of the Eclipse Public License 2.0
* which is available at https://www.eclipse.org/legal/epl-2.0/
*
* SPDX-License-Identifier: EPL-2.0
**********************************************************************/
package fr.jmini.asciidoctorj.dynamicinclude;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.asciidoctor.Asciidoctor;
import org.asciidoctor.Asciidoctor.Factory;
import org.asciidoctor.AttributesBuilder;
import org.asciidoctor.OptionsBuilder;
import org.junit.jupiter.api.Test;

public class Example1Test {

    @Test
    public void testIndex() throws Exception {
        runTest("index");
    }

    @Test
    public void testIndex1() throws Exception {
        runTest("index1");
    }

    @Test
    public void testIndex2() throws Exception {
        runTest("index2");
    }

    private void runTest(String fileName) throws IOException, URISyntaxException {
        Path exampleFolder = Paths.get("src/test/resources/example1")
                .toAbsolutePath();
        String content = new String(Files.readAllBytes(exampleFolder.resolve(fileName + ".adoc")), StandardCharsets.UTF_8);
        String expected = new String(Files.readAllBytes(exampleFolder.resolve(fileName + ".html")), StandardCharsets.UTF_8);

        Asciidoctor asciidoctor = Factory.create();
        AttributesBuilder attributesBuilder = AttributesBuilder.attributes()
                .sectionNumbers(true);

        OptionsBuilder optionsBuilder = OptionsBuilder.options()
                .attributes(attributesBuilder)
                .baseDir(exampleFolder.toFile())
                .docType("article");
        String html = asciidoctor.convert(content, optionsBuilder);

        assertThat(html).isEqualTo(expected);
    }
}
