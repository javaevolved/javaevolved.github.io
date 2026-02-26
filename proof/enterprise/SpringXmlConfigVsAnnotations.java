/// Proof: spring-xml-config-vs-annotations
/// Source: content/enterprise/spring-xml-config-vs-annotations.yaml
///
/// Note: Uses stub annotations to prove the configuration style compiles
/// without Spring dependency.
@interface SpringBootApplication {}
@interface Repository {}
@interface Service {}

interface JdbcTemplate {}

@SpringBootApplication
class Application {
    public static void main(String[] args) {
        // SpringApplication.run(Application.class, args);
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
