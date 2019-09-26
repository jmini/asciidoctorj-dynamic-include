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

public class AbsolutePathComparator implements Comparator<Path> {

    private Map<Path, List<String>> orderMap = new HashMap<>();
    private Function<Path, List<String>> orderSupplier;
    private Set<String> messages = new HashSet<>();

    public AbsolutePathComparator(Function<Path, List<String>> orderSupplier) {
        this.orderSupplier = orderSupplier;
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

        List<String> order = orderMap.computeIfAbsent(commonPath, orderSupplier);
        if (order != null) {
            if (!order.contains("index")) {
                if ("index".equals(nameWithoutSuffix1)) {
                    if ("index".equals(nameWithoutSuffix2)) {
                        return name1.compareTo(name2);
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
                        return name1.compareTo(name2);
                    }
                    return result;
                } else {
                    messages.add("No ordering indication for '" + nameWithoutSuffix2 + "' in '" + commonPath + "', putting it at the end");
                    return 1;
                }
            } else {
                messages.add("No ordering indication for '" + nameWithoutSuffix1 + "' in '" + commonPath + "', putting it at the end");
                if (order.contains(nameWithoutSuffix2)) {
                    return -1;
                }
            }
        }
        if ("index".equals(nameWithoutSuffix1)) {
            return -1;
        } else if ("index".equals(nameWithoutSuffix2)) {
            return 1;
        }
        return name1.compareTo(name2);
    }

    public Set<String> getMessages() {
        return messages;
    }

}
