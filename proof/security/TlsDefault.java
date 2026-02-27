///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25+
import java.net.http.*;
import javax.net.ssl.*;

/// Proof: tls-default
/// Source: content/security/tls-default.yaml
void main() throws Exception {
    // TLS 1.3 is the default!
    var client = HttpClient.newBuilder()
        .sslContext(SSLContext.getDefault())
        .build();
    // Already using TLS 1.3
}
