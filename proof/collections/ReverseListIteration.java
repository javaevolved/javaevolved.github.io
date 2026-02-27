///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25+
import java.util.*;

/// Proof: reverse-list-iteration
/// Source: content/collections/reverse-list-iteration.yaml
void main() {
    List<String> list = List.of("a", "b", "c");
    for (String element : list.reversed()) {
        System.out.println(element);
    }
}
