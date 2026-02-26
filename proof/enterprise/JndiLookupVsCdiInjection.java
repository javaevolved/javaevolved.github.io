import java.sql.*;
import java.util.*;

/// Proof: jndi-lookup-vs-cdi-injection
/// Source: content/enterprise/jndi-lookup-vs-cdi-injection.yaml
@interface ApplicationScoped {}
@interface Inject {}
@interface Resource { String name() default ""; }

interface DataSource {
    Connection getConnection() throws SQLException;
}

record Order(String id) {}

@ApplicationScoped
class OrderService {
    @Inject
    @Resource(name = "jdbc/OrderDB")
    DataSource ds;

    public List<Order> findAll()
            throws SQLException {
        try (Connection con = ds.getConnection()) {
            // query orders
        }
        return List.of();
    }
}

void main() {}
