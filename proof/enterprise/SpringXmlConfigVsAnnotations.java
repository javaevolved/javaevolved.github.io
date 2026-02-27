///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25+
//DEPS org.springframework.boot:spring-boot-autoconfigure:3.4.3
//DEPS org.springframework.boot:spring-boot:3.4.3
//DEPS org.springframework:spring-context:7.0.5
//DEPS org.springframework:spring-jdbc:7.0.5

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;

/// Proof: spring-xml-config-vs-annotations
/// Source: content/enterprise/spring-xml-config-vs-annotations.yaml
@SpringBootApplication
class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}

@Repository
class UserRepository {
    private final JdbcTemplate jdbc;

    public UserRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }
}

@Service
class UserService {
    private final UserRepository repository;

    public UserService(UserRepository repository) {
        this.repository = repository;
    }
}

void main() {}
