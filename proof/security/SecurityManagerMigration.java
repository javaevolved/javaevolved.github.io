///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25+
import java.nio.file.*;

/// Proof: security-manager-migration
/// Source: content/security/security-manager-migration.yaml
void main() throws Exception {
    Path allowedRoot = Files.createTempDirectory("allowed-root");
    Path requested = Path.of("document.txt");
    Files.writeString(allowedRoot.resolve(requested), "authorized");

    // Untrusted users must not be able to modify this tree.
    Path root = allowedRoot.toRealPath();
    Path resolved = root.resolve(requested)
        .normalize()
        .toRealPath();
    if (!resolved.startsWith(root)) {
        throw new SecurityException(
            "Path is outside the allowed root");
    }
    String content = Files.readString(resolved);

    Files.delete(resolved);
    Files.delete(allowedRoot);
}
