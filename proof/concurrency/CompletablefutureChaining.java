///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25+
import java.util.concurrent.*;

/// Proof: completablefuture-chaining
/// Source: content/concurrency/completablefuture-chaining.yaml
String fetchData() { return "data"; }
String transform(String data) { return data.toUpperCase(); }

void main() {
    CompletableFuture.supplyAsync(
        this::fetchData
    )
    .thenApply(this::transform)
    .thenAccept(System.out::println);
}
