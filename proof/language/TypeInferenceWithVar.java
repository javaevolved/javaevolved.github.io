///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25+
import java.util.*;

/// Proof: type-inference-with-var
/// Source: content/language/type-inference-with-var.yaml
void main() {
    var map = new HashMap<String, List<Integer>>();
    for (var entry : map.entrySet()) {
        // clean and readable
    }
}
