import java.util.*;

/// Proof: require-nonnull-else
/// Source: content/errors/require-nonnull-else.yaml
void main() {
    String input = null;
    String name = Objects
        .requireNonNullElse(
            input, "default"
        );
}
