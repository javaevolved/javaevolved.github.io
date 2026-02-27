///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25+
import module java.base;

/// Proof: module-import-declarations
/// Source: content/language/module-import-declarations.yaml
void main() {
    // All of java.util, java.io, java.nio
    // etc. available in one line
    var list = new ArrayList<String>();
    list.add("hello");
}
