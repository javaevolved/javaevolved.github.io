import java.util.*;
import java.util.function.*;

/// Proof: jdbc-resultset-vs-jpa-criteria
/// Source: content/enterprise/jdbc-resultset-vs-jpa-criteria.yaml
@interface PersistenceContext {}

record User(String status, int age) {}

interface CriteriaQuery<T> {
    CriteriaQuery<T> select(Root<T> root);
    CriteriaQuery<T> where(Object... predicates);
    Root<T> from(Class<T> cls);
}
interface Root<T> {
    <Y> Path<Y> get(String name);
}
interface Path<Y> {}
interface CriteriaBuilder {
    <T> CriteriaQuery<T> createQuery(Class<T> cls);
    Object equal(Path<?> path, Object value);
    Object greaterThan(Path<? extends Comparable> path, Comparable value);
}
interface TypedQuery<T> {
    List<T> getResultList();
}
interface EntityManager {
    CriteriaBuilder getCriteriaBuilder();
    <T> TypedQuery<T> createQuery(CriteriaQuery<T> q);
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
