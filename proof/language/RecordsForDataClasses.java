///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25+
/// Proof: records-for-data-classes
/// Source: content/language/records-for-data-classes.yaml
public record Point(int x, int y) {}

void main() {
    var p = new Point(1, 2);
}
