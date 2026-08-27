///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25+
import java.util.concurrent.*;

/// Proof: thread-stop-to-cooperative-cancellation
/// Source: content/concurrency/thread-stop-to-cooperative-cancellation.yaml
void main() throws Exception {
    Runnable runTask = () -> {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                Thread.sleep(10);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    };

    try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
        Future<?> worker = executor.submit(runTask);

        // Requests interruption; the task must cooperate.
        worker.cancel(true);
    }
}
