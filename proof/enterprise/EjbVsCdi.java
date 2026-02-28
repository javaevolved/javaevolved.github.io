///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25+
//DEPS jakarta.enterprise:jakarta.enterprise.cdi-api:4.1.0
//DEPS jakarta.transaction:jakarta.transaction-api:2.0.1

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

/// Proof: ejb-vs-cdi
/// Source: content/enterprise/ejb-vs-cdi.yaml
record Order(Object item) {}

class InventoryService {
    void reserve(Object item) {}
}

@ApplicationScoped
class OrderService {
    @Inject
    private InventoryService inventory;

    @Transactional
    public void placeOrder(Order order) {
        inventory.reserve(order.item());
    }
}

void main() {}
