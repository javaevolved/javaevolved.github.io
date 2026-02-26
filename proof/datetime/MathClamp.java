/// Proof: math-clamp
/// Source: content/datetime/math-clamp.yaml
void main() {
    int value = 150;
    int clamped =
        Math.clamp(value, 0, 100);
    // value constrained to [0, 100]
}
