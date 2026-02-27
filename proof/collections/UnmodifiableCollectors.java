///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25+
import java.util.*;
import java.util.stream.*;

/// Proof: unmodifiable-collectors
/// Source: content/collections/unmodifiable-collectors.yaml
void main() {
    Stream<String> stream = Stream.of("a", "b", "c");
    List<String> list = stream.toList();
}
