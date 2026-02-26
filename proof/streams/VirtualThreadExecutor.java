import java.util.*;
import java.util.concurrent.*;

/// Proof: virtual-thread-executor
/// Source: content/streams/virtual-thread-executor.yaml
interface Task { String run() throws Exception; }

void main() throws Exception {
    List<Task> tasks = List.of(() -> "result1", () -> "result2");
    try (var exec = Executors
            .newVirtualThreadPerTaskExecutor()) {
        var futures = tasks.stream()
            .map(t -> exec.submit(t::run))
            .toList();
    }
}
