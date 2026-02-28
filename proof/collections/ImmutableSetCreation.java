///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25+
import java.util.*;

/// Proof: immutable-set-creation
/// Source: content/collections/immutable-set-creation.yaml
void main() {
    Set<String> set =
        Set.of("a", "b", "c");
}
