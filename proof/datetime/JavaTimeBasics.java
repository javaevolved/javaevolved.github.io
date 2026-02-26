import java.time.*;

/// Proof: java-time-basics
/// Source: content/datetime/java-time-basics.yaml
void main() {
    LocalDate date = LocalDate.of(
        2025, Month.JANUARY, 15);
    LocalTime time = LocalTime.of(14, 30);
    Instant now = Instant.now();
    // immutable, thread-safe
}
