import java.util.stream.*;

/// Proof: stream-tolist
/// Source: content/streams/stream-tolist.yaml
void main() {
    Stream<String> stream = Stream.of("hello", "hi", "world");
    List<String> result = stream
        .filter(s -> s.length() > 3)
        .toList();
}
