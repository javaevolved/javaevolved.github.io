///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25+
//DEPS org.jspecify:jspecify:1.0.0

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import java.util.*;

/// Proof: spring-null-safety-jspecify
/// Source: content/enterprise/spring-null-safety-jspecify.yaml
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
