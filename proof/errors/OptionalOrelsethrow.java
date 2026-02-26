import java.util.*;

/// Proof: optional-orelsethrow
/// Source: content/errors/optional-orelsethrow.yaml
void main() {
    Optional<String> optional = Optional.of("value");
    // Clear intent: throws NoSuchElementException if empty
    String value = optional.orElseThrow();
}
