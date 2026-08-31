package hexlet.code;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.NoResultException;
import jakarta.transaction.Transactional;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class UrlRepositoryJdbc {

    private final DataSource ds;
    @PersistenceContext
    private EntityManager entityManager;
    public UrlRepositoryJdbc(DataSource ds) {
        this.ds = ds;
    }

    public List<UrlRow> findAll() {
        String sql = "SELECT id, base_url FROM urls ORDER BY id";

        try (var c = ds.getConnection();
             var ps = c.prepareStatement(sql);
             var rs = ps.executeQuery()) {

            List<UrlRow> out = new ArrayList<>();

            while (rs.next()) {
                out.add(new UrlRow(
                        rs.getLong("id"),
                        rs.getString("base_url")
                ));
            }
            return out;

        } catch (SQLException e) {
            throw new RuntimeException("Ошибка при получении списка URL", e);
        }
    }

    public Optional<UrlRow> findById(long id) {
        String sql = "SELECT id, base_url FROM urls WHERE id = ?";

        try (var c = ds.getConnection();
             var ps = c.prepareStatement(sql)) {

            ps.setLong(1, id);

            try (var rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }

                return Optional.of(new UrlRow(
                        rs.getLong("id"),
                        rs.getString("base_url")
                ));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Ошибка при поиске URL с id=" + id, e);
        }
    }

    public Optional<UrlCheck> findByUrl(String baseUrl) {
        try {
            var check = entityManager.createQuery("""
                SELECT c
                FROM UrlCheck c
                JOIN c.url u
                WHERE u.name = :baseUrl
                ORDER BY c.createdAt DESC
                """, UrlCheck.class)
                    .setParameter("baseUrl", baseUrl)
                    .setMaxResults(1)
                    .getSingleResult();
            return Optional.of(check);
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }
    @Transactional
    public UrlCheck upsertLikeCheck(String baseUrl) {
        return findByUrl(baseUrl)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Для URL ещё нет проверки: " + baseUrl
                        )
                );
    }

    public record UrlRow(long id, String baseUrl) {
    }
}
