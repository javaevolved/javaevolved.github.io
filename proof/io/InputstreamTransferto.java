import java.io.*;
import java.io.InputStream;
import java.io.OutputStream;

/// Proof: inputstream-transferto
/// Source: content/io/inputstream-transferto.yaml
void main() throws Exception {
    InputStream input = new java.io.ByteArrayInputStream("hello".getBytes());
    OutputStream output = new java.io.ByteArrayOutputStream();
    input.transferTo(output);
}
