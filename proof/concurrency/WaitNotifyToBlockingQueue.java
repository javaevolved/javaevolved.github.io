///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25+
import java.util.concurrent.*;

/// Proof: wait-notify-to-blocking-queue
/// Source: content/concurrency/wait-notify-to-blocking-queue.yaml
void main() throws Exception {
    record Job(String name) {}

    BlockingQueue<Job> queue = new LinkedBlockingQueue<>();
    queue.put(new Job("build"));

    Job job = queue.take();
    process(job);
}

void process(Object job) {
    System.out.println("processing " + job);
}
