import java.util.*;

/// Proof: optional-ifpresentorelse
/// Source: content/streams/optional-ifpresentorelse.yaml
record User(String name) {}

Optional<User> findUser(String id) {
    return id.equals("known") ? Optional.of(new User("Alice")) : Optional.empty();
}

void greet(User u) { System.out.println("Hello " + u.name()); }
void handleMissing() { System.out.println("not found"); }

void main() {
    String id = "known";
    findUser(id).ifPresentOrElse(
        this::greet,
        this::handleMissing
    );
}
