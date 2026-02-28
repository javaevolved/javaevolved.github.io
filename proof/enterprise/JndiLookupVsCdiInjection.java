///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25+
//DEPS jakarta.enterprise:jakarta.enterprise.cdi-api:4.1.0
//DEPS jakarta.annotation:jakarta.annotation-api:3.0.0

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.annotation.Resource;
import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

/// Proof: jndi-lookup-vs-cdi-injection
/// Source: content/enterprise/jndi-lookup-vs-cdi-injection.yaml
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
