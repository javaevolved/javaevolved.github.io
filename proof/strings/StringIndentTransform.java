/// Proof: string-indent-transform
/// Source: content/strings/string-indent-transform.yaml
void main() {
    String text = "hello world";
    String indented = text.indent(4);

    String result = text
        .transform(String::strip)
        .transform(s -> s.replace(" ", "-"));
}
