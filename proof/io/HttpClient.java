///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25+
import java.net.*;
import java.net.http.*;

/// Proof: http-client
/// Source: content/io/http-client.yaml
void main() throws Exception {
    var client = HttpClient.newHttpClient();
    var request = HttpRequest.newBuilder()
        .uri(URI.create("https://api.com/data"))
        .build();
    // Compilation proof — not executed
    // var response = client.send(
    //     request, BodyHandlers.ofString());
    // String body = response.body();
}
