import java.sql.*;

/// Proof: try-with-resources-effectively-final
/// Source: content/io/try-with-resources-effectively-final.yaml
void use(Connection conn) throws SQLException {}

Connection getConnection() throws SQLException {
    // Stub: return a no-op connection
    return new Connection() {
        public void close() {}
        public java.sql.Statement createStatement() { return null; }
        public java.sql.PreparedStatement prepareStatement(String s) { return null; }
        public java.sql.CallableStatement prepareCall(String s) { return null; }
        public String nativeSQL(String s) { return s; }
        public void setAutoCommit(boolean b) {}
        public boolean getAutoCommit() { return true; }
        public void commit() {}
        public void rollback() {}
        public boolean isClosed() { return false; }
        public java.sql.DatabaseMetaData getMetaData() { return null; }
        public void setReadOnly(boolean b) {}
        public boolean isReadOnly() { return false; }
        public void setCatalog(String c) {}
        public String getCatalog() { return null; }
        public void setTransactionIsolation(int l) {}
        public int getTransactionIsolation() { return 0; }
        public java.sql.SQLWarning getWarnings() { return null; }
        public void clearWarnings() {}
        public java.sql.Statement createStatement(int r, int c) { return null; }
        public java.sql.PreparedStatement prepareStatement(String s, int r, int c) { return null; }
        public java.sql.CallableStatement prepareCall(String s, int r, int c) { return null; }
        public java.util.Map<String, Class<?>> getTypeMap() { return null; }
        public void setTypeMap(java.util.Map<String, Class<?>> m) {}
        public void setHoldability(int h) {}
        public int getHoldability() { return 0; }
        public java.sql.Savepoint setSavepoint() { return null; }
        public java.sql.Savepoint setSavepoint(String n) { return null; }
        public void rollback(java.sql.Savepoint s) {}
        public void releaseSavepoint(java.sql.Savepoint s) {}
        public java.sql.Statement createStatement(int r, int c, int h) { return null; }
        public java.sql.PreparedStatement prepareStatement(String s, int r, int c, int h) { return null; }
        public java.sql.CallableStatement prepareCall(String s, int r, int c, int h) { return null; }
        public java.sql.PreparedStatement prepareStatement(String s, int a) { return null; }
        public java.sql.PreparedStatement prepareStatement(String s, int[] ci) { return null; }
        public java.sql.PreparedStatement prepareStatement(String s, String[] cn) { return null; }
        public java.sql.Clob createClob() { return null; }
        public java.sql.Blob createBlob() { return null; }
        public java.sql.NClob createNClob() { return null; }
        public java.sql.SQLXML createSQLXML() { return null; }
        public boolean isValid(int t) { return true; }
        public void setClientInfo(String n, String v) {}
        public void setClientInfo(java.util.Properties p) {}
        public String getClientInfo(String n) { return null; }
        public java.util.Properties getClientInfo() { return null; }
        public java.sql.Array createArrayOf(String t, Object[] e) { return null; }
        public java.sql.Struct createStruct(String t, Object[] a) { return null; }
        public void setSchema(String s) {}
        public String getSchema() { return null; }
        public void abort(java.util.concurrent.Executor e) {}
        public void setNetworkTimeout(java.util.concurrent.Executor e, int ms) {}
        public int getNetworkTimeout() { return 0; }
        public <T> T unwrap(Class<T> iface) { return null; }
        public boolean isWrapperFor(Class<?> iface) { return false; }
    };
}

void main() throws Exception {
    Connection conn = getConnection();
    // Use existing variable directly
    try (conn) {
        use(conn);
    }
}
