///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25+
import java.util.*;

/// Proof: optional-or
/// Source: content/streams/optional-or.yaml
record Config(String value) {}

Optional<Config> primary() { return Optional.empty(); }
Optional<Config> secondary() { return Optional.empty(); }
Optional<Config> defaults() { return Optional.of(new Config("default")); }

void main() {
    Optional<Config> cfg = primary()
        .or(this::secondary)
        .or(this::defaults);
}
