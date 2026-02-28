///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25+
//DEPS jakarta.enterprise:jakarta.enterprise.cdi-api:4.1.0
//DEPS org.eclipse.microprofile.reactive.messaging:microprofile-reactive-messaging-api:3.0.1

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.reactive.messaging.Incoming;

/// Proof: mdb-vs-reactive-messaging
/// Source: content/enterprise/mdb-vs-reactive-messaging.yaml
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
