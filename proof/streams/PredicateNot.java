import java.util.*;
import java.util.function.*;

/// Proof: predicate-not
/// Source: content/streams/predicate-not.yaml
void main() {
    List<String> list = List.of("hello", "", "world", "  ");
    List<String> nonEmpty = list.stream()
        .filter(Predicate.not(String::isBlank))
        .toList();
}
