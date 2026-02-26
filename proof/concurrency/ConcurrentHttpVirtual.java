import java.net.*;
import java.net.http.*;
import java.util.*;
import java.util.concurrent.*;

/// Proof: concurrent-http-virtual
/// Source: content/concurrency/concurrent-http-virtual.yaml
HttpRequest req(String url) throws Exception {
    return HttpRequest.newBuilder(new URI(url)).build();
}

void main() throws Exception {
    var client = HttpClient.newHttpClient();
    var urls = List.<String>of();  // empty — proof is compile-only
    try (var exec = Executors
        .newVirtualThreadPerTaskExecutor()) {
        var results = urls.stream()
            .map(u -> exec.submit(() -> {
                try {
                    return client.send(req(u),
                        HttpResponse.BodyHandlers.ofString()).body();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }))
            .toList().stream()
            .map(f -> {
                try { return f.get(); }
                catch (Exception e) { throw new RuntimeException(e); }
            }).toList();
    }
}
