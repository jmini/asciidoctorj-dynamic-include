package fr.jmini.asciidoctorj.dynamicinclude;

import java.nio.file.Path;

public class FileHolder {

    private Path path;
    private String pathScope;
    private String pathArea;
    private String key;
    private String content;
    private TitleType titleType;
    private String title;
    private int titleLevel;
    private String titleId;
    private int titleStart;
    private int titleEnd;

    public FileHolder(Path path, String key, String pathScope, String pathArea, String content, TitleType titleType, String title, int titleLevel, String titleId, int titleStart, int titleEnd) {
        this.path = path;
        this.key = key;
        this.pathScope = pathScope;
        this.pathArea = pathArea;
        this.content = content;
        this.titleType = titleType;
        this.title = title;
        this.titleLevel = titleLevel;
        this.titleId = titleId;
        this.titleStart = titleStart;
        this.titleEnd = titleEnd;
    }

    public Path getPath() {
        return path;
    }

    public String getKey() {
        return key;
    }

    public String getPathScope() {
        return pathScope;
    }

    public String getPathArea() {
        return pathArea;
    }

    public String getContent() {
        return content;
    }

    public TitleType getTitleType() {
        return titleType;
    }

    public String getTitle() {
        return title;
    }

    public int getTitleLevel() {
        return titleLevel;
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
