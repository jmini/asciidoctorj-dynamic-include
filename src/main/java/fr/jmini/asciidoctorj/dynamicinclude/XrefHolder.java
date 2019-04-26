package fr.jmini.asciidoctorj.dynamicinclude;

public class XrefHolder {
    private String file;
    private String anchor;
    private String text;
    private XrefHolderType type;
    private int startIndex;
    private int endIndex;

    public XrefHolder(String file, String anchor, String text, XrefHolderType type, int startIndex, int endIndex) {
        super();
        this.file = file;
        this.anchor = anchor;
        this.text = text;
        this.type = type;
        this.startIndex = startIndex;
        this.endIndex = endIndex;
    }

    public String getFile() {
        return file;
    }

    public String getAnchor() {
        return anchor;
    }

    public String getText() {
        return text;
    }

    public XrefHolderType getType() {
        return type;
    }

    public int getStartIndex() {
        return startIndex;
    }

    public int getEndIndex() {
        return endIndex;
    }

    public static enum XrefHolderType {
        DOUBLE_ANGLED_BRACKET, INLINE, TEXT
    }
}
