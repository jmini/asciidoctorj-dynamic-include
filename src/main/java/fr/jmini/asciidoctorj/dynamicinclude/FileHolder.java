package fr.jmini.asciidoctorj.dynamicinclude;

import java.nio.file.Path;

public class FileHolder {

    private Path path;
    private String nameWithoutSuffix;
    private String nameSuffix;
    private String key;
    private String content;
    private TitleType titleType;
    private String title;
    private int titleLevel;
    private String titleId;
    private int titleStart;
    private int titleEnd;

    public FileHolder(Path path, String key, String nameWithoutSuffix, String nameSuffix, String content, TitleType titleType, String title, int titleLevel, String titleId, int titleStart, int titleEnd) {
        this.path = path;
        this.key = key;
        this.nameWithoutSuffix = nameWithoutSuffix;
        this.nameSuffix = nameSuffix;
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

    public String getNameWithoutSuffix() {
        return nameWithoutSuffix;
    }

    public String getNameSuffix() {
        return nameSuffix;
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
