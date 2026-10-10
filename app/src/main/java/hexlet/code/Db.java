package hexlet.code;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class Db {
    private Db() {

    }

    public static DataSource dataSource() {
        final int maxPoolSize = 10;
        final int valFailTimeout = 5000;
        HikariConfig config = new HikariConfig();

        config.setJdbcUrl("jdbc:h2:mem:project;DB_CLOSE_DELAY=-1;MODE=PostgreSQL");

        config.setUsername("sa");
        config.setPassword("");

        config.setPoolName("h2-hikari");
        config.setMaximumPoolSize(maxPoolSize);

        // по желанию: чтобы при старте была проверка соединения
        config.setInitializationFailTimeout(valFailTimeout);

        return new HikariDataSource(config);
    }
    public static Connection getConnection() throws SQLException {
        String jdbcUrl = System.getenv("JDBC_DATABASE_URL");
        if (jdbcUrl == null || jdbcUrl.isBlank()) {
            throw new IllegalStateException("JDBC_DATABASE_URL is not set");
        }

        // Если вы полностью передаете user/password в URL — username/password не нужны
        return DriverManager.getConnection(jdbcUrl);
    }
}
