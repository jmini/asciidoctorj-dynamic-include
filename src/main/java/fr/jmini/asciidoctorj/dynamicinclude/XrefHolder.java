package fr.jmini.asciidoctorj.dynamicinclude;

public class XrefHolder {
    private String file;
    private String anchor;
    private String text;
    private boolean doubleAngledBracketForm;
    private int startIndex;
    private int endIndex;

    public XrefHolder(String file, String anchor, String text, boolean doubleAngledBracketForm, int startIndex, int endIndex) {
        super();
        this.file = file;
        this.anchor = anchor;
        this.text = text;
        this.doubleAngledBracketForm = doubleAngledBracketForm;
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

    public boolean isDoubleAngledBracketForm() {
        return doubleAngledBracketForm;
    }

    public int getStartIndex() {
        return startIndex;
    }

    public int getEndIndex() {
        return endIndex;
    }
}
