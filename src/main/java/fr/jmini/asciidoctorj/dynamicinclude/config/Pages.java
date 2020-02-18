package fr.jmini.asciidoctorj.dynamicinclude.config;

import java.util.List;

/**
 * This is the object representation of the content of a `pages.yaml` file.
 */
public class Pages implements SortConfig {

    private List<String> order;
    private Order defaultOrder;

    @Override
    public List<String> getOrder() {
        return order;
    }

    public void setOrder(List<String> order) {
        this.order = order;
    }

    @Override
    public Order getDefaultOrder() {
        return defaultOrder;
    }

    public void setDefaultOrder(Order defaultOrder) {
        this.defaultOrder = defaultOrder;
    }
}
