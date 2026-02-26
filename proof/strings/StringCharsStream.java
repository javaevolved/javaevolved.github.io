/// Proof: string-chars-stream
/// Source: content/strings/string-chars-stream.yaml
void process(char c) {}

void main() {
    String str = "abc123";
    str.chars()
        .filter(Character::isDigit)
        .forEach(c -> process((char) c));
}
