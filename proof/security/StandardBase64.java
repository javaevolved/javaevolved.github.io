///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25+
import java.util.Base64;

/// Proof: standard-base64
/// Source: content/security/standard-base64.yaml
void main() {
    byte[] data = "Java evolved".getBytes();

    String encoded = Base64.getEncoder()
        .encodeToString(data);
}
