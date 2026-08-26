///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25+
import java.util.ArrayDeque;
import java.util.Deque;

/// Proof: stack-to-deque
/// Source: content/collections/stack-to-deque.yaml
void main() {
    Deque<String> stack = new ArrayDeque<>();
    stack.push("task");
    String next = stack.pop();
}
