///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25+
import java.util.*;
import java.util.stream.*;
import static java.util.stream.Collectors.*;

/// Proof: collectors-teeing
/// Source: content/collections/collectors-teeing.yaml
record Item(String name, double price) {}
record Stats(long count, double total) {}

void main() {
    var items = List.of(new Item("a", 10.0), new Item("b", 20.0));
    var result = items.stream().collect(
        Collectors.teeing(
            Collectors.counting(),
            Collectors.summingDouble(Item::price),
            Stats::new
        )
    );
}
