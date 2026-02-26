import java.security.*;

/// Proof: strong-random
/// Source: content/security/strong-random.yaml
void main() throws Exception {
    // Platform's strongest algorithm
    SecureRandom random =
        SecureRandom.getInstanceStrong();
    byte[] bytes = new byte[32];
    random.nextBytes(bytes);
}
