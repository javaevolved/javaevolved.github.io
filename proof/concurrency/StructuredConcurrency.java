import java.util.concurrent.*;

/// Proof: structured-concurrency
/// Source: content/concurrency/structured-concurrency.yaml
record User(String name) {}
record Order(String id) {}
record Result(User user, Order order) {}

User fetchUser() { return new User("Alice"); }
Order fetchOrder() { return new Order("o1"); }
Result combine(User u, Order o) { return new Result(u, o); }

void main() throws Exception {
    try (var scope = StructuredTaskScope.open()) {
        var u = scope.fork(this::fetchUser);
        var o = scope.fork(this::fetchOrder);
        scope.join();
        combine(u.get(), o.get());
    }
}
