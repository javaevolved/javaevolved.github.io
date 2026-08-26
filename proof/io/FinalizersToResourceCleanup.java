///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25+

import java.lang.ref.Cleaner;

/// Proof: finalizers-to-resource-cleanup
/// Source: content/io/finalizers-to-resource-cleanup.yaml
final class NativeResource implements AutoCloseable {
    private static final Cleaner CLEANER = Cleaner.create();

    private static final class State implements Runnable {
        private final long handle;

        State(long handle) {
            this.handle = handle;
        }

        @Override
        public void run() {
            release(handle);
        }
    }

    private final Cleaner.Cleanable cleanable;

    NativeResource(long handle) {
        cleanable = CLEANER.register(this, new State(handle));
    }

    private static void release(long handle) {
        IO.println("Released native handle " + handle);
    }

    @Override
    public void close() {
        cleanable.clean();
    }
}

void main() {
    try (var resource = new NativeResource(42L)) {
        IO.println("Using native resource");
    }
}
