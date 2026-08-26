///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25+
import java.util.*;

/// Proof: collection-bulk-operations
/// Source: content/collections/collection-bulk-operations.yaml
void main() {
    List<Order> orders = new ArrayList<>(List.of(
        new Order(false),
        new Order(true)
    ));

    orders.removeIf(Order::cancelled);
}

record Order(boolean cancelled) {}
