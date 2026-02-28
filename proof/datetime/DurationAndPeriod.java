///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25+
import java.time.*;
import java.time.temporal.*;

/// Proof: duration-and-period
/// Source: content/datetime/duration-and-period.yaml
void main() {
    LocalDate date1 = LocalDate.of(2024, 1, 1);
    LocalDate date2 = LocalDate.of(2025, 1, 1);
    LocalTime time1 = LocalTime.of(8, 0);
    LocalTime time2 = LocalTime.of(17, 0);

    long days = ChronoUnit.DAYS
        .between(date1, date2);
    Period period = Period.between(
        date1, date2);
    Duration elapsed = Duration.between(
        time1, time2);
}
