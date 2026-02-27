///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25+
/// Proof: string-isblank
/// Source: content/strings/string-isblank.yaml
void main() {
    String str = "  \t  ";
    boolean blank = str.isBlank();
    // handles Unicode whitespace too
}
