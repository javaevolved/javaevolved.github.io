/// Proof: string-isblank
/// Source: content/strings/string-isblank.yaml
void main() {
    String str = "  \t  ";
    boolean blank = str.isBlank();
    // handles Unicode whitespace too
}
