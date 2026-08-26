///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25+
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/// Proof: legacy-synchronized-collections
/// Source: content/collections/legacy-synchronized-collections.yaml
void main() {
    String id = "session-1";
    Session session = new Session();

    ConcurrentMap<String, Session> sessions = new ConcurrentHashMap<>();
    sessions.put(id, session);
}

record Session() {}
