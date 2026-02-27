///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25+
/// Proof: pattern-matching-instanceof
/// Source: content/language/pattern-matching-instanceof.yaml
void main() {
    Object obj = "Hello, world!";
    if (obj instanceof String s) {
        System.out.println(s.length());
    }
}
