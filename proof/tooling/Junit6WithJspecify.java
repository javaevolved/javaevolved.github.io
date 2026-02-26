/// Proof: junit6-with-jspecify
/// Source: content/tooling/junit6-with-jspecify.yaml
///
/// Note: Uses stub annotations to prove the null-safety + JUnit pattern
/// compiles without JUnit 6 and JSpecify dependencies.
@interface Test {}
@interface NullMarked {}
@interface Nullable {}

record User(String name) {}

interface UserService {
    @Nullable User findById(String id);
}

void assertNotNull(Object obj) { if (obj == null) throw new AssertionError(); }
void assertNull(Object obj) { if (obj != null) throw new AssertionError(); }
void assertEquals(Object a, Object b) {}

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
