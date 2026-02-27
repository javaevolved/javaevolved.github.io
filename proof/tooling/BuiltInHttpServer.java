///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25+
import java.net.*;
import java.nio.file.*;
import com.sun.net.httpserver.*;

/// Proof: built-in-http-server
/// Source: content/tooling/built-in-http-server.yaml
void main() throws Exception {
    // Or use the API (JDK 18+)
    var server = SimpleFileServer.createFileServer(
        new InetSocketAddress(0),
        Path.of(System.getProperty("java.io.tmpdir")).toAbsolutePath(),
        SimpleFileServer.OutputLevel.VERBOSE);
    // server.start(); // not started in proof
}
