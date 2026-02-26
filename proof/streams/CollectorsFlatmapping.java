import java.util.*;
import java.util.stream.*;
import static java.util.stream.Collectors.*;

/// Proof: collectors-flatmapping
/// Source: content/streams/collectors-flatmapping.yaml
record Emp(String dept, List<String> tags) {}

void main() {
    var employees = List.of(
        new Emp("eng", List.of("java", "cloud")),
        new Emp("eng", List.of("java", "ai"))
    );
    var tagsByDept = employees.stream()
        .collect(groupingBy(
            Emp::dept,
            flatMapping(
                e -> e.tags().stream(),
                toSet()
            )
        ));
}
