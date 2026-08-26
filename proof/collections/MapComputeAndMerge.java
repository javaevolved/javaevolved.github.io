///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25+
import java.util.*;

/// Proof: map-compute-and-merge
/// Source: content/collections/map-compute-and-merge.yaml
void main() {
    Map<String, List<String>> groups = new HashMap<>();
    String key = "language";
    String value = "Java";

    groups.computeIfAbsent(key, ignored -> new ArrayList<>())
            .add(value);
}
