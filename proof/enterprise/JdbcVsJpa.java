import java.util.*;

/// Proof: jdbc-vs-jpa
/// Source: content/enterprise/jdbc-vs-jpa.yaml
@interface PersistenceContext {}

record User(Long id, String name) {}

interface TypedQuery<T> {
    TypedQuery<T> setParameter(String name, Object value);
    List<T> getResultList();
}
interface EntityManager {
    <T> T find(Class<T> cls, Object id);
    <T> TypedQuery<T> createQuery(String jpql, Class<T> cls);
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
