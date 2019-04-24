package fr.jmini.asciidoctorj.dynamicinclude;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.asciidoctor.log.LogHandler;
import org.asciidoctor.log.LogRecord;

public class InMemoryLogHanlder implements LogHandler {

    private List<LogRecord> logs = new ArrayList<>();

    @Override
    public void log(LogRecord logRecord) {
        logs.add(logRecord);
    }

    public List<LogRecord> getLogs() {
        return Collections.unmodifiableList(logs);
    }
}
