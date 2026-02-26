/// Proof: default-interface-methods
/// Source: content/language/default-interface-methods.yaml
import java.io.*;

public interface Logger {
    default void log(String msg) {
        System.out.println(
            timestamp() + ": " + msg);
    }
    String timestamp();
}

// Multiple interfaces allowed
public class FileLogger
        implements Logger, Closeable {
    public String timestamp() {
        return java.time.Instant.now().toString();
    }
    public void close() {}
}

void main() {
    new FileLogger().log("hello");
}
