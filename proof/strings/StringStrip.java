/// Proof: string-strip
/// Source: content/strings/string-strip.yaml
void main() {
    String str = "  hello world  ";
    // strip() removes all Unicode whitespace
    String clean = str.strip();
    String left  = str.stripLeading();
    String right = str.stripTrailing();
}
