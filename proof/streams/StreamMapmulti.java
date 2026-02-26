import java.util.*;
import java.util.stream.*;

/// Proof: stream-mapmulti
/// Source: content/streams/stream-mapmulti.yaml
record Order(String id, List<String> items) {}
record OrderItem(String orderId, String item) {}

void main() {
    var orders = List.of(
        new Order("o1", List.of("a", "b")),
        new Order("o2", List.of("c"))
    );
    Stream<Order> stream = orders.stream();
    stream.<OrderItem>mapMulti(
        (order, downstream) -> {
            for (var item : order.items())
                downstream.accept(
                    new OrderItem(order.id(), item));
        }
    ).toList();
}
