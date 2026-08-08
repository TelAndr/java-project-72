package hexlet.code;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Statement;
public class DataSourceFactory {
    public static DataSource create() {
        // Переключение логикой: если задан DB_URL — берём PostgreSQL
        // иначе используем H2.
        String dbUrl = env("DB_URL", null);

        if (dbUrl != null && !dbUrl.isBlank()) {
            // PostgreSQL
            HikariConfig cfg = new HikariConfig();
            cfg.setJdbcUrl(dbUrl);
            cfg.setUsername(env("DB_USER", ""));
            cfg.setPassword(env("DB_PASSWORD", ""));
            cfg.setMaximumPoolSize(10);
            return new HikariDataSource(cfg);
        }

        // Dev: H2 (по умолчанию)
        // DB_CLOSE_DELAY=-1 чтобы БД жила в течение процесса
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl("jdbc:h2:file:./devdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL");
        cfg.setUsername("sa");
        cfg.setPassword("");
        cfg.setMaximumPoolSize(5);
        return new HikariDataSource(cfg);
    }

    public static void initSchema(DataSource ds) {
        String ddl = readResource("schema.sql");
        if (ddl == null || ddl.isBlank()) return;

        try (Connection c = ds.getConnection();
             Statement st = c.createStatement()) {
            st.execute(ddl);
        } catch (Exception e) {
            throw new RuntimeException("Failed to init schema", e);
        }
    }

    private static String readResource(String name) {
        try (InputStream is = DataSourceFactory.class.getClassLoader().getResourceAsStream(name)) {
            if (is == null) return null;
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String env(String key, String defaultValue) {
        String v = System.getenv(key);
        if (v == null) v = System.getProperty(key);
        return (v == null || v.isBlank()) ? defaultValue : v;
    }
}
