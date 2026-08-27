///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25+
import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/// Proof: explicit-charset-file-io
/// Source: content/io/explicit-charset-file-io.yaml
void main() throws Exception {
    var path = Path.of(System.getProperty("java.io.tmpdir"), "proof-charset.txt");
    Files.writeString(path, "content", StandardCharsets.UTF_8);

    try (BufferedReader reader = Files.newBufferedReader(
            path, StandardCharsets.UTF_8)) {
        readAll(reader);
    }
}

String readAll(BufferedReader reader) throws Exception {
    var sb = new StringBuilder();
    String line;
    while ((line = reader.readLine()) != null) {
        sb.append(line);
    }
    return sb.toString();
}
