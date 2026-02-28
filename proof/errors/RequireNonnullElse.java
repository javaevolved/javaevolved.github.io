///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25+
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
