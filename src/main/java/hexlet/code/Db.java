package hexlet.code;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
public class Db {
    private Db() {}

    public static DataSource dataSource() {
        HikariConfig config = new HikariConfig();

        // Имя in-memory базы = "project"
        config.setJdbcUrl("jdbc:h2:mem:project;DB_CLOSE_DELAY=-1;MODE=PostgreSQL");

        config.setUsername("sa");
        config.setPassword("");

        config.setPoolName("h2-hikari");
        config.setMaximumPoolSize(10);

        // по желанию: чтобы при старте была проверка соединения
        config.setInitializationFailTimeout(5000);

        return new HikariDataSource(config);
    }
}
