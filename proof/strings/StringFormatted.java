/// Proof: string-formatted
/// Source: content/strings/string-formatted.yaml
void main() {
    String name = "Alice";
    int age = 30;
    String msg =
        "Hello %s, you are %d"
        .formatted(name, age);
}
