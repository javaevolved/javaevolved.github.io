///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25+
import java.time.*;

/// Proof: instant-precision
/// Source: content/datetime/instant-precision.yaml
void main() {
    // Microsecond/nanosecond precision
    Instant now = Instant.now();
    // 2025-02-15T20:12:25.678901234Z
    long nanos = now.getNano();
}
