///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25+
import java.util.stream.*;

/// Proof: stream-iterate-predicate
/// Source: content/streams/stream-iterate-predicate.yaml
void main() {
    Stream.iterate(
        1,
        n -> n < 1000,
        n -> n * 2
    ).forEach(System.out::println);
    // stops when n >= 1000
}
