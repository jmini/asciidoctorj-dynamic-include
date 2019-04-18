/*******************************************************************************
 * Copyright (c) 2016 Jeremie Bresson.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jeremie Bresson - initial API and implementation
 ******************************************************************************/
package fr.jmini.asciidoctorj.dynamicinclude;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.asciidoctor.ast.ContentNode;
import org.asciidoctor.ast.Document;
import org.asciidoctor.extension.IncludeProcessor;
import org.asciidoctor.extension.PreprocessorReader;

public class DynamicIncludeProcessor extends IncludeProcessor {

    private static final String PREFIX = "dynamic:";

    public DynamicIncludeProcessor() {
        super();
    }

    public DynamicIncludeProcessor(Map<String, Object> config) {
        super(config);
    }

    @Override
    public boolean handles(String target) {
        return target.startsWith(PREFIX);
    }

    @Override
    public void process(Document document, PreprocessorReader reader, String target, Map<String, Object> attributes) {
        Path dir = Paths.get(reader.getDir());
        String glob = target.substring(PREFIX.length());

        List<Path> files = findFiles(dir, glob);

        String order;
        if (attributes.containsKey("order")) {
            order = attributes.get("order")
                    .toString();
        } else if (document.getAttributes()
                .containsKey("dynamic-include-order")) {
            order = document.getAttributes()
                    .get("dynamic-include-order")
                    .toString();
        } else {
            order = null;
        }

        List<String> orderList;
        if (order != null) {
            try {
                Path oderFile = dir.resolve(order);
                if (Files.isReadable(oderFile)) {
                    orderList = Files.readAllLines(oderFile);
                } else {
                    System.out.println("Could not find order file:" + oderFile.toAbsolutePath());
                    orderList = Collections.emptyList();
                }
            } catch (IOException e) {
                //TODO: do something else with the exception
                e.printStackTrace();
                orderList = Collections.emptyList();
            }
        } else {
            orderList = Collections.emptyList();
        }
        List<Path> list = sortList(files, orderList, p -> dir.relativize(p)
                .toString());

        for (int i = list.size() - 1; i >= 0; i--) {
            File file = list.get(i)
                    .toFile();
            reader.push_include(file.getName(), target + "(" + i + ")", file.getParent(), 1, attributes);
        }
    }

    public static List<Path> findFiles(Path dir, String glob) {
        final PathMatcher matcher = FileSystems.getDefault()
                .getPathMatcher("glob:" + dir + File.separator + glob);

        List<Path> result = new ArrayList<>();
        try {
            Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (matcher.matches(file)) {
                        result.add(file);
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            //TODO: do something else with the exception
            e.printStackTrace();
        }
        return Collections.unmodifiableList(result);
    }

    public static <T> List<T> sortList(List<T> list, List<String> orderedKeys, Function<T, String> keyExtractor) {
        return list.stream()
                .sorted(Comparator.comparingInt((T i) -> indexOfOrMaxValue(orderedKeys, keyExtractor.apply(i), list.size()))
                        .thenComparing(Comparator.comparing(keyExtractor)))
                .collect(Collectors.toList());
    }

    private static int indexOfOrMaxValue(List<String> orderedKeys, String value, int maxValue) {
        int index = orderedKeys.indexOf(value);
        if (index == -1) {
            return maxValue;
        }
        return index;
    }

    //    @Override
    //    public Object process(ContentNode parent, String target, Map<String, Object> attributes) {
    //        Object order = searchAttribute(attributes, "order", "1", parent, "include-order");
    //        Object scopes = searchAttribute(attributes, "scopes", "2", parent, "include-scopes");
    //        Object topics = searchAttribute(attributes, "topics", "3", parent, "include-topics");
    //
    //        System.out.println(target);
    //        System.out.println(attributes);
    //
    //        create
    //        Document document = asciidoctor.load("content", new HashMap<>());
    //
    //        return document.convert();
    //        //        GitLink link = GitLinkUtility.compute(path, mode, server, repository, branch, linkText, docFile);
    //        //
    //        //        if (link.getWarning() != null) {
    //        //            //TODO: log a warning containing link.getWarning()
    //        //        }
    //        //
    //        //        if (link.getUrl() == null) {
    //        //            return link.getText();
    //        //        } else {
    //        //            // Define options for an 'anchor' element:
    //        //            Map<String, Object> options = new HashMap<String, Object>();
    //        //            options.put("type", ":link");
    //        //            options.put("target", link.getUrl());
    //        //
    //        //            // Define attribute for an 'anchor' element:
    //        //            if (linkWindow != null && !linkWindow.toString()
    //        //                    .isEmpty()) {
    //        //                attributes.put("window", linkWindow.toString());
    //        //            }
    //        //
    //        //            // Create the 'anchor' node:
    //        //            PhraseNode inline = createPhraseNode(parent, "anchor", link.getText(), attributes, options);
    //        //
    //        //            // Convert to String value:
    //        //            return inline.convert();
    //        //        }
    //        //        inline.convert();
    //    }
    //
    //    private Object searchAttribute(Map<String, Object> attributes, String attrKey, String attrPosition, ContentNode parent, String attrDocumentKey) {
    //        Object result;
    //        //Try to get the attribute by key:
    //        result = attributes.get(attrKey);
    //        if (result != null) {
    //            return result;
    //        }
    //        //Try to get the attribute by position:
    //        result = attributes.get(attrPosition);
    //        if (result != null) {
    //            return result;
    //        }
    //        //Try to get the attribute in the document:
    //        if (attrDocumentKey != null) {
    //            return parent.getDocument()
    //                    .getAttribute(attrDocumentKey);
    //        }
    //        //Not found:
    //        return null;
    //    }
    //
    private String searchDocFile(ContentNode parent) {
        Map<Object, Object> options = parent.getDocument()
                .getOptions();
        for (Object optionKey : options.keySet()) {

            if ("attributes".equals(optionKey)) {
                Object attributesObj = options.get(optionKey);
                if (attributesObj instanceof Map<?, ?>) {
                    Map<?, ?> attributes = (Map<?, ?>) attributesObj;
                    Object docfile = attributes.get("docfile");
                    if (docfile != null) {
                        return docfile.toString();
                    }
                }
            }
        }
        return null;
    }
}
