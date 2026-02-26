import java.util.*;

/// Proof: sequenced-collections
/// Source: content/collections/sequenced-collections.yaml
void main() {
    List<String> list = new ArrayList<>(List.of("a", "b", "c"));
    var last = list.getLast();
    var first = list.getFirst();
    var reversed = list.reversed();
}
