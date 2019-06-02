package fr.jmini.asciidoctorj.dynamicinclude;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class MessageCollector implements Consumer<String> {

    private List<String> messages = new ArrayList<>();

    @Override
    public void accept(String message) {
        messages.add(message);
    }

    public List<String> getMessages() {
        return Collections.unmodifiableList(messages);
    }

}
