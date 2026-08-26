///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25+

/// Proof: stack-walker
/// Source: content/tooling/stack-walker.yaml
String callerClass() {
    return StackWalker.getInstance()
            .walk(frames -> frames.skip(1)
                    .findFirst()
                    .orElseThrow()
                    .getClassName());
}

void main() {
    IO.println(callerClass());
}
