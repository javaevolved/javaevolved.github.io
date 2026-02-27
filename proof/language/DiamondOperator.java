///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25+
import java.util.*;
import java.util.function.*;

/// Proof: diamond-operator
/// Source: content/language/diamond-operator.yaml
void main() {
    Map<String, List<String>> map =
        new HashMap<>();
    // Java 9: diamond with anonymous classes
    Predicate<String> p =
        new Predicate<>() {
            public boolean test(String s) { return !s.isEmpty(); }
        };
}
