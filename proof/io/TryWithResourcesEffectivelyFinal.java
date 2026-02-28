///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25+
//DEPS com.h2database:h2:2.3.232

import java.sql.*;

/// Proof: try-with-resources-effectively-final
/// Source: content/io/try-with-resources-effectively-final.yaml
void use(Connection conn) throws SQLException {}

Connection getConnection() throws SQLException {
    return DriverManager.getConnection("jdbc:h2:mem:");
}

void main() throws Exception {
    Connection conn = getConnection();
    // Use existing variable directly
    try (conn) {
        use(conn);
    }
}
