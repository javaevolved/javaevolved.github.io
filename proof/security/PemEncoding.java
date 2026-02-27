///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25+
//JAVAC_OPTIONS --enable-preview --release 25
//JAVA_OPTIONS --enable-preview
import java.security.*;

/// Proof: pem-encoding
/// Source: content/security/pem-encoding.yaml
void main() throws Exception {
    KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
    kpg.initialize(2048);
    KeyPair kp = kpg.generateKeyPair();

    // Encode to PEM
    String pem = PEMEncoder.of()
        .encodeToString(kp.getPublic());
    // Decode from PEM
    var cert = PEMDecoder.of()
        .decode(pem);
}
