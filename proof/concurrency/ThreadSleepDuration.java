///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25+
import java.time.*;

/// Proof: thread-sleep-duration
/// Source: content/concurrency/thread-sleep-duration.yaml
void main() throws InterruptedException {
    Thread.sleep(
        Duration.ofMillis(1)
    );
    Thread.sleep(
        Duration.ofMillis(1)
    );
}
