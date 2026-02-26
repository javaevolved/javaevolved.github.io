import java.util.function.*;

/// Proof: lock-free-lazy-init
/// Source: content/concurrency/lock-free-lazy-init.yaml
///
/// Note: The snippet calls INST.get() on a StableValue<Config>. In JDK 25,
/// StableValue.supplier() returns a Supplier<T> whose get() provides the same
/// lock-free lazy initialization semantics.
class Config {
    private static final Supplier<Config> INST =
        StableValue.supplier(Config::load);

    static Config get() {
        return INST.get();
    }

    static Config load() {
        return null; // placeholder — real load reads from disk/DB
    }
}

void main() {
    Config.get();
}
