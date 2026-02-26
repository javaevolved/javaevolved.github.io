import java.util.*;

/// Proof: spring-null-safety-jspecify
/// Source: content/enterprise/spring-null-safety-jspecify.yaml
///
/// Note: Uses stub annotations to prove the null-safety pattern compiles
/// without JSpecify dependency.
@interface NullMarked {}
@interface Nullable {}

record User(String name) {}

interface UserRepository {
    Optional<User> findById(String id);
    List<User> findAll();
    User save(User user);
}

@NullMarked
class UserService {
    UserRepository repository;

    public @Nullable User findById(String id) {
        return repository.findById(id).orElse(null);
    }

    public List<User> findAll() {
        return repository.findAll();
    }

    public User save(User user) {
        return repository.save(user);
    }
}

void main() {}
