/// Proof: hex-format
/// Source: content/datetime/hex-format.yaml
void main() {
    byte byteValue = 0x48;
    var hex = HexFormat.of()
        .withUpperCase();
    String s = hex.toHexDigits(
        byteValue);
    byte[] bytes =
        hex.parseHex("48656C6C6F");
}
