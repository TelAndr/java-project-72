package hexlet.code;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
public class JdbcUtil {
    private static String setting(String property, String env, String defaultValue) {
        String value = System.getProperty(property);
        if (value == null || value.isBlank()) {
            value = System.getenv(env);
        }
        return value == null || value.isBlank() ? defaultValue : value;
    }
    public static DataSource createDataSource() {
        final int maxPoolSize = 5;
        //String url = System.getenv().getOrDefault("JDBC_URL", "jdbc:postgresql://localhost:5432/app");
        //String user = System.getenv().getOrDefault("JDBC_USER", "app");
        //String pass = System.getenv().getOrDefault("JDBC_PASS", "andrey1987");

        String url = setting("db.jdbcUrl", "JDBC_URL",
                "jdbc:postgresql://localhost:5432/app");
        System.out.println("JDBC URL: " + url);
        String user = setting("db.user", "JDBC_USER", "app");
        String pass = setting("db.password", "JDBC_PASS", "andrey1987");

        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(url);
        cfg.setUsername(user);
        cfg.setPassword(pass);
        cfg.setMaximumPoolSize(maxPoolSize);
        return new HikariDataSource(cfg);
    }

    public static void ensureSeedUrlExists(Connection connection, long id, String address) throws SQLException {
        //try (Connection c = ds.getConnection()) {
            // "insert into urls(id, address) values(?, ?) on conflict (id) do nothing"
            // upsert-подход можно заменить под вашу БД
        String product = connection.getMetaData().getDatabaseProductName();

        String sql;
        if (product.toLowerCase().contains("h2")) {
            sql = """
            MERGE INTO urls (id, address)
            KEY (id)
            VALUES (?, ?)
            """;
        } else if (product.toLowerCase().contains("postgresql")) {
            sql = """
            INSERT INTO urls (id, address)
            VALUES (?, ?)
            ON CONFLICT (id) DO NOTHING
            """;
        } else {
            throw new SQLException("Unsupported database: " + product);
        }
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.setString(2, address);
            ps.executeUpdate();
        }
        //}
    }

    public static String findUrlAddress(DataSource ds, long id) throws SQLException {
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement("select address from urls where id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return rs.getString(1);
            }
        }
    }

    public static void insertCheck(DataSource ds, long urlId, String type, String remoteResponse) throws SQLException {
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "insert into checks(url_id, type, remote_response) values(?, ?, ?)"
             )) {
            int couterInd = 1;
            ps.setLong(couterInd, urlId);
            ps.setString(++couterInd, type);
            ps.setString(++couterInd, remoteResponse);
            ps.executeUpdate();
        }
    }
    public static String[] getChecksForUrl(DataSource ds, long urlId) throws SQLException {
        // если хотите вернуть "строки" — сделаем проще в сервлете без DTO,
        // но лучше вернуть список объектов. Для краткости вернём ResultSet-строки через String.
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "select id, type, remote_response,"
                             + "created_at from checks where url_id = ? order by created_at desc"
             )) {
            ps.setLong(1, urlId);
            //try (ResultSet rs = ps.executeQuery()) {
                // Собирать будем в сервлете, поэтому тут не используем.
                // Этот метод не нужен — оставьте только следующий helper ниже.
            //}
        }
        return new String[0];
    }

    public static void streamChecksForUrl(
            DataSource ds, long urlId, ChecksConsumer consumer
    ) throws SQLException {
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "select id, type, remote_response, created_at"
                             + "from checks where url_id = ? order by created_at desc"
             )) {
            ps.setLong(1, urlId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    long checkId = rs.getLong("id");
                    String type = rs.getString("type");
                    String remoteResponse = rs.getString("remote_response");
                    Timestamp createdAt = rs.getTimestamp("created_at");
                    consumer.accept(checkId, type, remoteResponse, createdAt);
                }
            }
        }
    }

    public static void insertUrlCheck(DataSource ds, long urlId, int statusCode) throws SQLException {
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "insert into url_checks(url_id, status_code) values(?, ?)"
             )) {
            ps.setLong(1, urlId);
            ps.setInt(2, statusCode);
            ps.executeUpdate();
        }
    }

    @FunctionalInterface
    public interface ChecksConsumer {
        void accept(long checkId, String type, String remoteResponse, java.sql.Timestamp createdAt);
    }
}
