/// Proof: string-lines
/// Source: content/strings/string-lines.yaml
void main() {
    String text = "one\ntwo\nthree";
    text.lines().forEach(System.out::println);
}
