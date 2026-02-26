import java.io.*;
import java.sql.*;
import java.text.*;

/// Proof: multi-catch
/// Source: content/errors/multi-catch.yaml
void process() throws IOException, SQLException, ParseException {}
void log(Exception e) { System.err.println(e); }

void main() {
    try {
        process();
    } catch (IOException
        | SQLException
        | ParseException e) {
        log(e);
    }
}
