import java.nio.file.*;

/// Proof: files-mismatch
/// Source: content/io/files-mismatch.yaml
void main() throws Exception {
    var path1 = Path.of(System.getProperty("java.io.tmpdir"), "proof-a.txt");
    var path2 = Path.of(System.getProperty("java.io.tmpdir"), "proof-b.txt");
    Files.writeString(path1, "hello");
    Files.writeString(path2, "hello");
    long pos = Files.mismatch(path1, path2);
    // -1 if identical
    // otherwise: position of first difference
}
