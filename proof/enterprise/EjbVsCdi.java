/// Proof: ejb-vs-cdi
/// Source: content/enterprise/ejb-vs-cdi.yaml
@interface ApplicationScoped {}
@interface Inject {}
@interface Transactional {}

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
