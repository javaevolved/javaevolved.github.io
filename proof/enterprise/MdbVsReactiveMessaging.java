/// Proof: mdb-vs-reactive-messaging
/// Source: content/enterprise/mdb-vs-reactive-messaging.yaml
@interface ApplicationScoped {}
@interface Incoming { String value(); }

record Order(String id) {}

void fulfillOrder(Order order) {}

@ApplicationScoped
class OrderProcessor {
    @Incoming("orders")
    public void process(Order order) {
        // automatically deserialized from
        // the "orders" channel
        fulfillOrder(order);
    }
}

void main() {}
