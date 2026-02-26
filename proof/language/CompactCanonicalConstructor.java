import java.util.*;

/// Proof: compact-canonical-constructor
/// Source: content/language/compact-canonical-constructor.yaml
public record Person(String name, List<String> pets) {
    // Compact constructor
    public Person {
        Objects.requireNonNull(name);
        pets = List.copyOf(pets);
    }
}

void main() {
    var p = new Person("Alice", List.of("cat", "dog"));
}
