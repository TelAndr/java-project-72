import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.sql.*;

public class JdbcUtil {

    public static DataSource createDataSource() {
        String url = System.getenv().getOrDefault("JDBC_URL", "jdbc:postgresql://localhost:5432/app");
        String user = System.getenv().getOrDefault("JDBC_USER", "app");
        String pass = System.getenv().getOrDefault("JDBC_PASS", "app");

        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(url);
        cfg.setUsername(user);
        cfg.setPassword(pass);
        cfg.setMaximumPoolSize(5);
        return new HikariDataSource(cfg);
    }

    public static void ensureSeedUrlExists(DataSource ds, long id, String address) throws SQLException {
        try (Connection c = ds.getConnection()) {
            // upsert-подход можно заменить под вашу БД
            try (PreparedStatement ps = c.prepareStatement(
                    "insert into urls(id, address) values(?, ?) on conflict (id) do nothing"
            )) {
                ps.setLong(1, id);
                ps.setString(2, address);
                ps.executeUpdate();
            }
        }
    }

    public static String findUrlAddress(DataSource ds, long id) throws SQLException {
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement("select address from urls where id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return rs.getString(1);
            }
        }
    }

    public static void insertCheck(DataSource ds, long urlId, String type, String remoteResponse) throws SQLException {
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "insert into checks(url_id, type, remote_response) values(?, ?, ?)"
             )) {
            ps.setLong(1, urlId);
            ps.setString(2, type);
            ps.setString(3, remoteResponse);
            ps.executeUpdate();
        }
    }
    public static String[] getChecksForUrl(DataSource ds, long urlId) throws SQLException {
        // если хотите вернуть "строки" — сделаем проще в сервлете без DTO,
        // но лучше вернуть список объектов. Для краткости вернём ResultSet-строки через String.
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "select id, type, remote_response, created_at from checks where url_id = ? order by created_at desc"
             )) {
            ps.setLong(1, urlId);
            try (ResultSet rs = ps.executeQuery()) {
                // Собирать будем в сервлете, поэтому тут не используем.
                // Этот метод не нужен — оставьте только следующий helper ниже.
            }
        }
        return new String[0];
    }

    public static void streamChecksForUrl(
            DataSource ds, long urlId, ChecksConsumer consumer
    ) throws SQLException {
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "select id, type, remote_response, created_at from checks where url_id = ? order by created_at desc"
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