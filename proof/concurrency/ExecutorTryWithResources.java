///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25+
import java.util.concurrent.*;

/// Proof: executor-try-with-resources
/// Source: content/concurrency/executor-try-with-resources.yaml
void main() throws Exception {
    Runnable task = () -> System.out.println("task");
    try (var exec =
            Executors.newCachedThreadPool()) {
        exec.submit(task);
    }
    // auto shutdown + await on close
}
