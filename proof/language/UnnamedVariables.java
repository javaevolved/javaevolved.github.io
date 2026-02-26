import java.util.*;

/// Proof: unnamed-variables
/// Source: content/language/unnamed-variables.yaml
void process(Object value) {}
void log(String msg) {}

void main() {
    String input = "123";
    Map<String, Integer> map = Map.of("a", 1);
    try {
        Integer.parseInt(input);
    } catch (Exception _) {
        log("parse failed");
    }
    map.forEach((_, value) -> {
        process(value);
    });
}
