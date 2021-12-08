package fr.jmini.asciidoctorj.dynamicinclude;

public class TitleHolder {

    private TitleType titleType;
    private int titleLevel;
    private String title;
    private String titleId;
    private int titleStart;
    private int titleEnd;

    public TitleHolder(TitleType titleType, int titleLevel, String title, String titleId, int titleStart, int titleEnd) {
        this.titleType = titleType;
        this.titleLevel = titleLevel;
        this.title = title;
        this.titleId = titleId;
        this.titleStart = titleStart;
        this.titleEnd = titleEnd;
    }

    public TitleType getTitleType() {
        return titleType;
    }

    public int getTitleLevel() {
        return titleLevel;
    }

    public String getTitle() {
        return title;
    }

    public String getTitleId() {
        return titleId;
    }

    public int getTitleStart() {
        return titleStart;
    }

    public int getTitleEnd() {
        return titleEnd;
    }
}
