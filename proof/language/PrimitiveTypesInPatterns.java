///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25+
//JAVAC_OPTIONS --enable-preview --release 25
//JAVA_OPTIONS --enable-preview
/// Proof: primitive-types-in-patterns
/// Source: content/language/primitive-types-in-patterns.yaml
String classify(int code) {
    return switch (code) {
        case int c when c >= 200
            && c < 300 -> "success";
        case int c when c >= 400
            && c < 500 -> "client error";
        default -> "other";
    };
}

void main() {
    classify(200);
    classify(404);
    classify(500);
}
