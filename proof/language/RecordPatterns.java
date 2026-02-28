///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25+
/// Proof: record-patterns
/// Source: content/language/record-patterns.yaml
record Point(int x, int y) {}

void main() {
    Object obj = new Point(3, 4);
    if (obj instanceof Point(int x, int y)) {
        System.out.println(x + y);
    }
}
