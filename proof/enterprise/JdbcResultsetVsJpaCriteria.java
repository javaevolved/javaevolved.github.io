///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25+
//DEPS jakarta.persistence:jakarta.persistence-api:3.2.0

import jakarta.persistence.*;
import jakarta.persistence.criteria.*;
import java.util.*;

/// Proof: jdbc-resultset-vs-jpa-criteria
/// Source: content/enterprise/jdbc-resultset-vs-jpa-criteria.yaml
class User {
    String status;
    int age;
}

class UserRepository {
    @PersistenceContext
    EntityManager em;

    public List<User> findActiveAboveAge(
            String status, int minAge) {
        var cb = em.getCriteriaBuilder();
        var cq =
            cb.createQuery(User.class);
        var root = cq.from(User.class);
        cq.select(root).where(
            cb.equal(root.get("status"), status),
            cb.greaterThan(root.get("age"), minAge));
        return em.createQuery(cq).getResultList();
    }
}

void main() {}
