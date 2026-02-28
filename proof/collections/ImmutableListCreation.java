///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25+
import java.util.*;

/// Proof: immutable-list-creation
/// Source: content/collections/immutable-list-creation.yaml
void main() {
    List<String> list =
        List.of("a", "b", "c");
}
