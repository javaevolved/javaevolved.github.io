///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25+
import java.util.*;

/// Proof: immutable-map-creation
/// Source: content/collections/immutable-map-creation.yaml
void main() {
    Map<String, Integer> map =
        Map.of("a", 1, "b", 2, "c", 3);
}
