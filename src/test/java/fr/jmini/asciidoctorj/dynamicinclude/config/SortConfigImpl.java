package fr.jmini.asciidoctorj.dynamicinclude.config;

import java.util.List;

public class SortConfigImpl implements SortConfig {

    private List<String> order;
    private Order defaultOrder;

    public SortConfigImpl(List<String> order, Order defaultOrder) {
        super();
        this.order = order;
        this.defaultOrder = defaultOrder;
    }

    @Override
    public List<String> getOrder() {
        return order;
    }

    @Override
    public Order getDefaultOrder() {
        return defaultOrder;
    }

}
