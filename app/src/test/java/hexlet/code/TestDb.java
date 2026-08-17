package hexlet.code;
import org.h2.tools.RunScript;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
public class TestDb {
    public static Connection connect() throws Exception {
        return DriverManager.getConnection(
                System.getProperty("db.jdbcUrl"),
                System.getProperty("db.user", "sa"),
                System.getProperty("db.password", "")
        );
    }
}
