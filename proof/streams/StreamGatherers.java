///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25+
import java.util.*;
import java.util.stream.*;

/// Proof: stream-gatherers
/// Source: content/streams/stream-gatherers.yaml
void main() {
    Stream<Integer> stream = Stream.of(1, 2, 3, 4, 5);
    var windows = stream
        .gather(
            Gatherers.windowSliding(3)
        )
        .toList();
}
