package fr.jmini.asciidoctorj.dynamicinclude;

import java.nio.file.Path;
import java.util.Map;

public class FileHolder {

    private Path path;
    private String nameWithoutSuffix;
    private String nameSuffix;
    private String key;
    private String content;
    private TitleHolder firstTitle;
    private int levelOffset;
    private Map<String, String> titleAnchorMap;
    private Map<String, String> anchorShift;

    public FileHolder(Path path, String key, String nameWithoutSuffix, String nameSuffix, String content, TitleHolder firstTitle, int levelOffset, Map<String, String> titleAnchorMap, Map<String, String> anchorShift) {
        this.path = path;
        this.key = key;
        this.nameWithoutSuffix = nameWithoutSuffix;
        this.nameSuffix = nameSuffix;
        this.content = content;
        this.firstTitle = firstTitle;
        this.levelOffset = levelOffset;
        this.titleAnchorMap = titleAnchorMap;
        this.anchorShift = anchorShift;
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

    public TitleHolder getFirstTitle() {
        return firstTitle;
    }

    public int getLevelOffset() {
        return levelOffset;
    }

    public Map<String, String> getTitleAnchorMap() {
        return titleAnchorMap;
    }

    public Map<String, String> getAnchorShift() {
        return anchorShift;
    }

}
