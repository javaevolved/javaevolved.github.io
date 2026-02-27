///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25+
import java.time.*;
import java.time.format.*;

/// Proof: date-formatting
/// Source: content/datetime/date-formatting.yaml
void main() {
    DateTimeFormatter fmt =
        DateTimeFormatter.ofPattern(
            "uuuu-MM-dd");
    String formatted =
        LocalDate.now().format(fmt);
    // Thread-safe, immutable
}
