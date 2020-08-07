package fr.jmini.asciidoctorj.dynamicinclude.path;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import fr.jmini.asciidoctorj.dynamicinclude.config.Order;
import fr.jmini.asciidoctorj.dynamicinclude.config.SortConfig;

public class AbsolutePathComparator implements Comparator<Path> {

    private static final Order DEFAULT_ORDER = Order.LEXICOGRAPHIC;
    private Map<Path, SortConfig> configMap = new HashMap<>();
    private Function<Path, SortConfig> sortConfigSupplier;
    private Set<String> messages = new HashSet<>();
    private List<String> suffixes;

    public AbsolutePathComparator(Function<Path, SortConfig> sortConfigSupplier, List<String> suffixes) {
        this.sortConfigSupplier = sortConfigSupplier;
        this.suffixes = suffixes;
    }

    @Override
    public int compare(Path p1, Path p2) {
        if (Objects.equals(p1, p2)) {
            return 0;
        }
        Path commonPath = PathUtil.getCommonPath(p1, p2);
        String name1 = commonPath.relativize(p1)
                .getName(0)
                .toString();
        String nameWithoutSuffix1 = PathUtil.getNameWithoutSuffix(name1);
        String name2 = commonPath.relativize(p2)
                .getName(0)
                .toString();
        String nameWithoutSuffix2 = PathUtil.getNameWithoutSuffix(name2);

        Order defaultOrder;
        SortConfig sortConfig = configMap.computeIfAbsent(commonPath, sortConfigSupplier);
        if (sortConfig != null) {
            List<String> order = sortConfig.getOrder();
            if (sortConfig.getDefaultOrder() != null) {
                defaultOrder = sortConfig.getDefaultOrder();
            } else {
                defaultOrder = DEFAULT_ORDER;
            }
            if (order != null) {
                if (!order.contains("index")) {
                    if ("index".equals(nameWithoutSuffix1)) {
                        if ("index".equals(nameWithoutSuffix2)) {
                            return compareNameAndSuffixes(name1, name2, defaultOrder);
                        }
                        return -1;
                    } else if ("index".equals(nameWithoutSuffix2)) {
                        return 1;
                    }
                }
                if (order.contains(nameWithoutSuffix1)) {
                    if (order.contains(nameWithoutSuffix2)) {
                        int result = order.indexOf(nameWithoutSuffix1) - order.indexOf(nameWithoutSuffix2);
                        if (result == 0) {
                            return compareNameAndSuffixes(name1, name2, defaultOrder);
                        }
                        return result;
                    } else {
                        messages.add("No ordering indication for '" + nameWithoutSuffix2 + "' in '" + commonPath + "', putting it at the end");
                        return -1;
                    }
                } else {
                    messages.add("No ordering indication for '" + nameWithoutSuffix1 + "' in '" + commonPath + "', putting it at the end");
                    if (order.contains(nameWithoutSuffix2)) {
                        return 1;
                    }
                }
            }
        } else {
            defaultOrder = DEFAULT_ORDER;
        }
        if ("index".equals(nameWithoutSuffix1)) {
            if ("index".equals(nameWithoutSuffix2)) {
                return compareNameAndSuffixes(name1, name2, defaultOrder);
            }
            return -1;
        } else if ("index".equals(nameWithoutSuffix2)) {
            return 1;
        }
        int result = compareNameWithoutSuffixes(nameWithoutSuffix1, nameWithoutSuffix2, defaultOrder);
        if (result == 0) {
            return compareNameAndSuffixes(name1, name2, defaultOrder);
        }
        return result;
    }

    private int compareNameAndSuffixes(String name1, String name2, Order defaultOrder) {
        String suffix1 = PathUtil.getNameSuffix(name1);
        String suffix2 = PathUtil.getNameSuffix(name2);
        if (suffixes.isEmpty() || Objects.equals(suffix1, suffix2)) {
            return compareNameWithoutSuffixes(name1, name2, defaultOrder);
        }
        if (suffix1 == null) {
            return -1;
        }
        if (suffix2 == null) {
            return 1;
        }
        return suffixes.indexOf(suffix1) - suffixes.indexOf(suffix2);
    }

    private int compareNameWithoutSuffixes(String name1, String name2, Order defaultOrder) {
        switch (defaultOrder) {
        case LEXICOGRAPHIC:
            return name1.compareTo(name2);
        case LEXICOGRAPHIC_REVERSED:
            return name2.compareTo(name1);
        case NATURAL:
            return AlphanumComparator.compare(name1, name2);
        case NATURAL_REVERSED:
            return AlphanumComparator.compare(name2, name1);
        default:
            throw new IllegalStateException("Illegal defaultOrder value: " + defaultOrder);
        }
    }

    public Set<String> getMessages() {
        return messages;
    }

}
