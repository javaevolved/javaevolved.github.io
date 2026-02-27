///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25+
//JAVAC_OPTIONS --enable-preview --release 25
//JAVA_OPTIONS --enable-preview
import java.util.function.*;
import java.util.logging.*;

/// Proof: stable-values
/// Source: content/concurrency/stable-values.yaml
///
/// Note: The snippet calls logger.get() on a StableValue<Logger>. In JDK 25,
/// StableValue.supplier() returns a Supplier<T> whose get() provides the same
/// lazy, thread-safe initialization semantics.
class LoggerHolder {
    private final Supplier<Logger> logger =
        StableValue.supplier(this::createLogger);

    Logger getLogger() {
        return logger.get();
    }

    Logger createLogger() {
        return Logger.getLogger(getClass().getName());
    }
}

void main() {
    new LoggerHolder().getLogger();
}
