import javax.crypto.*;
import javax.crypto.spec.*;
import java.security.*;

/// Proof: key-derivation-functions
/// Source: content/security/key-derivation-functions.yaml
void main() throws Exception {
    var kdf = KDF.getInstance("HKDF-SHA256");
    byte[] inputKeyBytes = new byte[32];
    byte[] salt = new byte[16];
    byte[] info = "context".getBytes();
    var inputKey = new SecretKeySpec(inputKeyBytes, "HKDF");
    SecretKey key = kdf.deriveKey(
        "AES",
        HKDFParameterSpec
            .ofExtract()
            .addIKM(inputKey)
            .addSalt(salt)
            .thenExpand(info, 32)
    );
}
