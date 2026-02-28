///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25+
import java.util.*;
import java.util.stream.*;

/// Proof: stream-takewhile-dropwhile
/// Source: content/streams/stream-takewhile-dropwhile.yaml
void main() {
    List<Integer> sorted = List.of(1, 5, 50, 100, 150, 200);
    var result = sorted.stream()
        .takeWhile(n -> n < 100)
        .toList();
    // or: .dropWhile(n -> n < 10)
}
