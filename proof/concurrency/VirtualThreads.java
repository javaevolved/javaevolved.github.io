///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25+
/// Proof: virtual-threads
/// Source: content/concurrency/virtual-threads.yaml
void main() throws InterruptedException {
    Thread.startVirtualThread(() -> {
        System.out.println("hello");
    }).join();
}
