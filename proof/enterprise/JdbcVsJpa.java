///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25+
//DEPS jakarta.persistence:jakarta.persistence-api:3.2.0

import jakarta.persistence.*;
import java.util.*;

/// Proof: jdbc-vs-jpa
/// Source: content/enterprise/jdbc-vs-jpa.yaml
class User {
    Long id; String name;
}

class UserRepository {
    @PersistenceContext
    EntityManager em;

    public User findUser(Long id) {
        return em.find(User.class, id);
    }

    public List<User> findByName(String name) {
        return em.createQuery(
            "SELECT u FROM User u WHERE u.name = :name",
            User.class)
            .setParameter("name", name)
            .getResultList();
    }
}

void main() {}
