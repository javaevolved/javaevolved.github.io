///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25+
import java.util.*;

/// Proof: copying-collections-immutably
/// Source: content/collections/copying-collections-immutably.yaml
void main() {
    List<String> original = new ArrayList<>(List.of("a", "b", "c"));
    List<String> copy =
        List.copyOf(original);
}
