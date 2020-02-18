package fr.jmini.asciidoctorj.dynamicinclude.config;

import java.util.List;

public interface SortConfig {

    List<String> getOrder();

    Order getDefaultOrder();

}
