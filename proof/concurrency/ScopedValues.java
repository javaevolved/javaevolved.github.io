///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25+
import java.util.concurrent.*;

/// Proof: scoped-values
/// Source: content/concurrency/scoped-values.yaml
record User(String name) {}
record Request(String path) {}

class Handler {
    static final ScopedValue<User> CURRENT =
        ScopedValue.newInstance();

    User authenticate(Request req) {
        return new User("Alice");
    }

    void handle(Request req) {
        ScopedValue.where(CURRENT,
            authenticate(req)
        ).run(this::process);
    }

    void process() {
        // use CURRENT.get() in scope
    }
}

void main() {
    new Handler().handle(new Request("/"));
}
