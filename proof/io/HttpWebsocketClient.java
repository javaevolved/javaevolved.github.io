///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25+
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.concurrent.CompletionStage;

/// Proof: http-websocket-client
/// Source: content/io/http-websocket-client.yaml
void main() {
    URI serverUri = URI.create("wss://example.com/socket");

    // Keep this compilation proof from opening a network connection.
    if (false) {
        HttpClient.newHttpClient()
            .newWebSocketBuilder()
            .buildAsync(serverUri,
                new WebSocket.Listener() {
                    @Override
                    public CompletionStage<?> onText(
                            WebSocket socket,
                            CharSequence data,
                            boolean last) {
                        handle(data.toString());
                        return WebSocket.Listener.super
                            .onText(socket, data, last);
                    }
                });
    }
}

void handle(String message) {
}
