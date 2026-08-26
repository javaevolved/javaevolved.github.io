///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25+
import java.util.*;

/// Proof: comparator-factories
/// Source: content/collections/comparator-factories.yaml
void main() {
    record Person(String name, int age) {}

    var people = new ArrayList<>(List.of(
            new Person("Ada", 36),
            new Person("Ada", 28),
            new Person("Grace", 40)));

    people.sort(Comparator.comparing(Person::name)
            .thenComparingInt(Person::age));
}
