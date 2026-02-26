/// Proof: virtual-threads
/// Source: content/concurrency/virtual-threads.yaml
void main() throws InterruptedException {
    Thread.startVirtualThread(() -> {
        System.out.println("hello");
    }).join();
}
