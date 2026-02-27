///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25+
import java.nio.file.*;

/// Proof: path-of
/// Source: content/io/path-of.yaml
void main() {
    var path = Path.of("src", "main",
        "java", "App.java");
}
