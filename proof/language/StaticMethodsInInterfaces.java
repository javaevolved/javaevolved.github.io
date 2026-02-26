/// Proof: static-methods-in-interfaces
/// Source: content/language/static-methods-in-interfaces.yaml
public interface Validator {
    boolean validate(String s);

    static boolean isBlank(String s) {
        return s == null ||
               s.trim().isEmpty();
    }
}

void main() {
    String input = "";
    // Usage
    if (Validator.isBlank(input)) {
        System.out.println("blank");
    }
}
