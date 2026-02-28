///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25+
import java.nio.file.*;

/// Proof: writing-files
/// Source: content/io/writing-files.yaml
void main() throws Exception {
    String content = "hello world";
    Files.writeString(
        Path.of(System.getProperty("java.io.tmpdir"), "proof-write.txt"),
        content
    );
}
