///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25+
import java.util.*;

/// Proof: raw-collections-to-generics
/// Source: content/language/raw-collections-to-generics.yaml
void main() {
    List<String> names = new ArrayList();
    names.add("Duke");

    String name = names.get(0);
}
