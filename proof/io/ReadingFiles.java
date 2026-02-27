///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25+
import java.nio.file.*;

/// Proof: reading-files
/// Source: content/io/reading-files.yaml
void main() throws Exception {
    var path = Path.of(System.getProperty("java.io.tmpdir"), "proof-read.txt");
    Files.writeString(path, "content");
    String content =
        Files.readString(path);
}
