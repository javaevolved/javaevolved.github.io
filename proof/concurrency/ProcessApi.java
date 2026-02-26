/// Proof: process-api
/// Source: content/concurrency/process-api.yaml
void main() {
    ProcessHandle ph =
        ProcessHandle.current();
    long pid = ph.pid();
    ph.info().command()
        .ifPresent(System.out::println);
    ph.children().forEach(
        c -> System.out.println(c.pid()));
}
