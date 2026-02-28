///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25+
/// Proof: pattern-matching-switch
/// Source: content/language/pattern-matching-switch.yaml
String format(Object obj) {
    return switch (obj) {
        case Integer i -> "int: " + i;
        case Double d  -> "double: " + d;
        case String s  -> "str: " + s;
        default        -> "unknown";
    };
}

void main() {
    format(42);
    format(3.14);
    format("hi");
}
