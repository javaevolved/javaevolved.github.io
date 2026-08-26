///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25+
import java.net.*;

/// Proof: url-constructors-to-uri
/// Source: content/io/url-constructors-to-uri.yaml
void main() throws Exception {
    URI endpointUri =
        URI.create("https://example.com/api?q=java");
    URL endpoint = endpointUri.toURL();
}
