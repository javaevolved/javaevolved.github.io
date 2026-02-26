import java.util.*;

/// Proof: jpa-vs-jakarta-data
/// Source: content/enterprise/jpa-vs-jakarta-data.yaml
@interface Repository {}

record User(Long id, String name) {}

interface CrudRepository<T, ID> {
    T findById(ID id);
    List<T> findAll();
    T save(T entity);
}

@Repository
interface Users extends CrudRepository<User, Long> {
    List<User> findByName(String name);
}

void main() {}
