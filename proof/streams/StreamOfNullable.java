import java.util.stream.*;

/// Proof: stream-of-nullable
/// Source: content/streams/stream-of-nullable.yaml
void main() {
    String val = null;
    Stream<String> s =
        Stream.ofNullable(val);
}
