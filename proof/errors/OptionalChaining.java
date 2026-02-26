import java.util.*;

/// Proof: optional-chaining
/// Source: content/errors/optional-chaining.yaml
record Address(String city) {}
record User(Address address) {}

Address address(User u) { return u.address(); }
String city(Address a) { return a.city(); }

void main() {
    User user = null;
    String city = Optional.ofNullable(user)
        .map(User::address)
        .map(Address::city)
        .orElse("Unknown");
}
