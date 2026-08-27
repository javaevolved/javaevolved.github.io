///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25+

/// Proof: runtime-exec-to-process-builder
/// Source: content/tooling/runtime-exec-to-process-builder.yaml
void main() throws Exception {
    String revision = "HEAD";

    Process process = new ProcessBuilder(
            "git", "show", revision)
            .redirectErrorStream(true)
            .start();

    try (var output = process.getInputStream()) {
        output.transferTo(OutputStream.nullOutputStream());
    }
    IO.println("exit code: " + process.waitFor());
}
