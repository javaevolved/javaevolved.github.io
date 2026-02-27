///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25+
//DEPS jakarta.data:jakarta.data-api:1.0.1

import jakarta.data.repository.*;
import java.util.*;

/// Proof: jpa-vs-jakarta-data
/// Source: content/enterprise/jpa-vs-jakarta-data.yaml
class User {
    Long id; String name;
}

@Repository
interface Users extends CrudRepository<User, Long> {
    List<User> findByName(String name);
}

void main() {}
