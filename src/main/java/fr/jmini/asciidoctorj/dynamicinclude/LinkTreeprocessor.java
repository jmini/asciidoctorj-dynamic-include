package fr.jmini.asciidoctorj.dynamicinclude;

import java.util.List;

//tag::include[]
import org.asciidoctor.ast.Block;
import org.asciidoctor.ast.Document;
import org.asciidoctor.ast.ListItem;
import org.asciidoctor.ast.StructuralNode;
import org.asciidoctor.extension.Treeprocessor;

public class LinkTreeprocessor extends Treeprocessor {

    @Override
    public Document process(Document document) {
        processBlock(document);
        return document;
    }

    private void processBlock(StructuralNode block) {
        List<StructuralNode> blocks = block.getBlocks();

        if (blocks != null) {
            for (int i = 0; i < blocks.size(); i++) {
                final StructuralNode currentBlock = blocks.get(i);
                if (currentBlock instanceof Block) {
                    Block b = (Block) currentBlock;
                    String source = b.getSource();
                    b.setSource(transform(source));
                } else if (currentBlock instanceof ListItem) {
                    ListItem li = (ListItem) currentBlock;
                    String source = li.getSource();
                    li.setSource(transform(source));
                } else {
                    // It's not a paragraph, so recursively descend into the child node
                    processBlock(currentBlock);
                }
            }
        }
    }

    public static String transform(String string) {
        return string;
    }
}
