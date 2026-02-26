import java.io.*;

/// Proof: deserialization-filters
/// Source: content/io/deserialization-filters.yaml
void main() throws Exception {
    var baos = new ByteArrayOutputStream();
    try (var oos = new ObjectOutputStream(baos)) {
        oos.writeObject("test");
    }
    var bais = new ByteArrayInputStream(baos.toByteArray());
    var ois = new ObjectInputStream(bais);

    ObjectInputFilter filter =
        ObjectInputFilter.Config
        .createFilter(
            "java.lang.*;!*"
        );
    ois.setObjectInputFilter(filter);
    Object obj = ois.readObject();
}
