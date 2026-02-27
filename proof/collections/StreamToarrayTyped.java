///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25+
import java.util.*;
import java.util.stream.*;

/// Proof: stream-toarray-typed
/// Source: content/collections/stream-toarray-typed.yaml
List<String> getNames() {
    return List.of("Alice", "Bob", "Charlie", "Dave");
}

void main() {
    String[] arr = getNames().stream()
        .filter(n -> n.length() > 3)
        .toArray(String[]::new);
}
