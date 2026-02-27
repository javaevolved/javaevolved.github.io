///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25+
//DEPS org.junit.jupiter:junit-jupiter-api:6.0.3
//DEPS org.jspecify:jspecify:1.0.0

import org.junit.jupiter.api.Test;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import static org.junit.jupiter.api.Assertions.*;

/// Proof: junit6-with-jspecify
/// Source: content/tooling/junit6-with-jspecify.yaml
record User(String name) {}

interface UserService {
    @Nullable User findById(String id);
}

UserService service = id -> id.equals("u1") ? new User("Alice") : null;

@NullMarked  // all refs non-null unless @Nullable
class UserServiceTest {

    // JUnit 6 API is @NullMarked:
    // assertNull(@Nullable Object actual)
    // assertEquals(@Nullable Object, @Nullable Object)
    // fail(@Nullable String message)

    @Test
    void findUser_found() {
        // IDE warns: findById returns @Nullable User
        @Nullable User result = service.findById("u1");
        assertNotNull(result); // narrows type to non-null
        assertEquals("Alice", result.name()); // safe
    }

    @Test
    void findUser_notFound() {
        @Nullable User result = service.findById("missing");
        assertNull(result); // IDE confirms null expectation
    }
}

void main() {}
