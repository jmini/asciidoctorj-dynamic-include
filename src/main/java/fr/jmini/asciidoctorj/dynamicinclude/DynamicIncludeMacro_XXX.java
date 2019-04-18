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

import java.util.Map;

import org.asciidoctor.Asciidoctor;
import org.asciidoctor.ast.ContentNode;
import org.asciidoctor.extension.InlineMacroProcessor;

public class DynamicIncludeMacro_XXX extends InlineMacroProcessor {

    private Asciidoctor asciidoctor;

    public DynamicIncludeMacro_XXX(Asciidoctor asciidoctor, String macroName, Map<String, Object> config) {
        super(macroName, config);
        this.asciidoctor = asciidoctor;
    }

    @Override
    public Object process(ContentNode parent, String target, Map<String, Object> attributes) {
        Object order = searchAttribute(attributes, "order", "1", parent, "include-order");
        Object scopes = searchAttribute(attributes, "scopes", "2", parent, "include-scopes");
        Object topics = searchAttribute(attributes, "topics", "3", parent, "include-topics");

        System.out.println(target);
        System.out.println(attributes);

        return "Bla XXX";
        //        create
        //        Document document = asciidoctor.load("content", new HashMap<>());

        //        return document.convert();
        //        GitLink link = GitLinkUtility.compute(path, mode, server, repository, branch, linkText, docFile);
        //
        //        if (link.getWarning() != null) {
        //            //TODO: log a warning containing link.getWarning()
        //        }
        //
        //        if (link.getUrl() == null) {
        //            return link.getText();
        //        } else {
        //            // Define options for an 'anchor' element:
        //            Map<String, Object> options = new HashMap<String, Object>();
        //            options.put("type", ":link");
        //            options.put("target", link.getUrl());
        //
        //            // Define attribute for an 'anchor' element:
        //            if (linkWindow != null && !linkWindow.toString()
        //                    .isEmpty()) {
        //                attributes.put("window", linkWindow.toString());
        //            }
        //
        //            // Create the 'anchor' node:
        //            PhraseNode inline = createPhraseNode(parent, "anchor", link.getText(), attributes, options);
        //
        //            // Convert to String value:
        //            return inline.convert();
        //        }
        //        inline.convert();
    }

    private Object searchAttribute(Map<String, Object> attributes, String attrKey, String attrPosition, ContentNode parent, String attrDocumentKey) {
        Object result;
        //Try to get the attribute by key:
        result = attributes.get(attrKey);
        if (result != null) {
            return result;
        }
        //Try to get the attribute by position:
        result = attributes.get(attrPosition);
        if (result != null) {
            return result;
        }
        //Try to get the attribute in the document:
        if (attrDocumentKey != null) {
            return parent.getDocument()
                    .getAttribute(attrDocumentKey);
        }
        //Not found:
        return null;
    }

    /**
     * @param parent
     * @return
     */
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
