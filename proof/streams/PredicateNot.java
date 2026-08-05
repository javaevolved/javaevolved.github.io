///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25+
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

/// Proof: predicate-not
/// Source: content/streams/predicate-not.yaml
void main() {
    List<String> list = List.of("hello", "", "world", "  ");
    List<String> nonEmpty = list.stream()
        .filter(Predicate.not(String::isBlank))
        .collect(Collectors.toList());
}
