///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25+
import java.util.concurrent.*;

/// Proof: timer-task-to-scheduled-executor
/// Source: content/concurrency/timer-task-to-scheduled-executor.yaml
void main() throws Exception {
    Runnable refresh = () -> System.out.println("refresh");

    ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor();

    ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(
            refresh, 0, 1, TimeUnit.MINUTES);

    future.cancel(false);
    scheduler.shutdown();
}
