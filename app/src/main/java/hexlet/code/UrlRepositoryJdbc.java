package hexlet.code;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.NoResultException;
import jakarta.transaction.Transactional;

import javax.sql.DataSource;
//import java.lang.module.Configuration;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.hibernate.cfg.Configuration;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.SessionFactory;

//import java.sql.Connection;
//import java.sql.PreparedStatement;
//import java.sql.ResultSet;

public class UrlRepositoryJdbc {

    private final DataSource ds;
    @PersistenceContext
    private EntityManager entityManager;
    public UrlRepositoryJdbc(DataSource ds) {
        this.ds = ds;
        StandardServiceRegistry registry = new StandardServiceRegistryBuilder()
                .applySetting("hibernate.connection.datasource", ds)
                .applySetting("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect")
                .applySetting("hibernate.hbm2ddl.auto", "update")
                .build();

        Metadata metadata = new MetadataSources(registry)
                .addAnnotatedClass(Url.class)
                .addAnnotatedClass(UrlCheck.class)
                .getMetadataBuilder()
                .build();
        SessionFactory sf = metadata.getSessionFactoryBuilder().build();
        this.entityManager = sf.createEntityManager();
    }
    /**
     * Возвращает все URL из таблицы {@code urls}, отсортированные по идентификатору
     * в порядке возрастания.
     *
     * @return список записей URL; если таблица пуста, возвращает пустой список
     * @throws RuntimeException если произошла ошибка при выполнении SQL-запроса
     */
    public List<UrlRow> findAll() {
        String sql = "SELECT id, address FROM urls ORDER BY id"; // base_url

        try (var c = ds.getConnection();
             var ps = c.prepareStatement(sql);
             var rs = ps.executeQuery()) {

            List<UrlRow> out = new ArrayList<>();

            while (rs.next()) {
                out.add(new UrlRow(
                        rs.getLong("id"),
                        rs.getString("address")
                ));
            }
            return out;

        } catch (SQLException e) {
            throw new RuntimeException("Ошибка при получении списка URL", e);
        }
    }
    /**
     * Ищет URL по идентификатору.
     *
     * @param id идентификатор URL
     * @return {@link Optional} с найденной записью либо {@link Optional#empty()},
     *         если запись с таким идентификатором отсутствует
     * @throws RuntimeException если произошла ошибка при выполнении SQL-запроса
     */
    public Optional<UrlRow> findById(long id) {
        String sql = "SELECT id, address FROM urls WHERE id = ?";

        try (var c = ds.getConnection();
             var ps = c.prepareStatement(sql)) {

            ps.setLong(1, id);

            try (var rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }

                return Optional.of(new UrlRow(
                        rs.getLong("id"),
                        rs.getString("address")
                ));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Ошибка при поиске URL с id=" + id, e);
        }
    }
    /**
     * Ищет последнюю проверку для URL с указанным именем.
     *
     * @param baseUrl имя URL, для которого выполняется поиск
     * @return {@link Optional} с самой последней проверкой или
     *         {@link Optional#empty()}, если проверок для URL нет
     */
    public Optional<Url> findByUrl(String baseUrl) {
        try {
            //var check = entityManager.createQuery("""
            //    SELECT c
            //    FROM UrlCheck c
            //    JOIN c.url u
            //    WHERE u.name = :baseUrl
            //    ORDER BY c.createdAt DESC
            //    """, UrlCheck.class)
            //        .setParameter("baseUrl", baseUrl)
            //        .setMaxResults(1)
            //        .getSingleResult();
            var url = entityManager.createQuery("""
            SELECT u
            FROM Url u
            WHERE u.name = :baseUrl
            """, Url.class)
                    .setParameter("baseUrl", baseUrl)
                    .getSingleResult();
            return Optional.of(url);
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    private Optional<UrlCheck> findByUrlCheck(String baseUrl) {
        String hql = "SELECT c FROM UrlCheck c JOIN c.url u WHERE u.name = :addr";
        return entityManager.createQuery(hql, UrlCheck.class)
                .setParameter("addr", baseUrl)
                .setMaxResults(1)
                .getResultList()
                .stream().findFirst();
    }

    @Transactional
    public Url upsertUrl(String baseUrl) {
        return findByUrl(baseUrl).map(existing -> {
            existing.setInsertedNew(false);
            return existing;
        }).orElseGet(() -> {
            Url u = new Url(baseUrl);
            u.setInsertedNew(true);
            entityManager.persist(u);
            entityManager.flush();
            return u;
        });
    }

    @Transactional
    public UrlCheck createCheck(Url url, Integer statusCode) {
        UrlCheck check = new UrlCheck();
        check.setUrl(url);
        check.setStatusCode(statusCode);   // уже заполнен
        check.setCreatedAt(OffsetDateTime.now());
        check.setInsertedNew(true);
        entityManager.persist(check);
        return check;
    }

    /**
     * Возвращает последнюю проверку для указанного URL.
     *
     * @param baseUrl имя URL, для которого требуется найти проверку
     * @return последняя проверка для URL
     * @throws IllegalArgumentException если для URL ещё нет проверок
     */
    //@Transactional
    //public UrlCheck upsertLikeCheck(String baseUrl) {
        //return findByUrl(baseUrl)
        //        .orElseThrow(() ->
        //                new IllegalArgumentException(
        //                        "Для URL ещё нет проверки: " + baseUrl
        //                )
        //        );
        // 1) Ищем существующую проверку по URL
    //    Optional<UrlCheck> existing = findByUrlCheck(baseUrl);
    //    if (existing.isPresent()) {
    //        existing.get().setInsertedNew(false);
    //        return existing.get();
    //    }

        // 2) URL ещё нет — создаём
    //    Url url = findByUrl(baseUrl).orElseGet(() -> {
    //        Url u = new Url(baseUrl);
    //        entityManager.persist(u);
    //        entityManager.flush();
    //        return u;
    //    });

        // 3) Создаём проверку
    //    UrlCheck check = new UrlCheck();
    //    check.setUrl(url);
    //    check.setCreatedAt(OffsetDateTime.now());
    //    check.setInsertedNew(true);
    //    entityManager.persist(check);

    //    return check;
    //}
    /**
     * Сохраняет заголовок документа для URL.
     *
     * @param url базовый URL, для которого сохраняется заголовок
     * @param titleDoc заголовок документа
     * @throws IllegalArgumentException если URL не найден
     * @throws RuntimeException если произошла ошибка при выполнении SQL-запроса
     */
    public void insertTitle(String url, String titleDoc) {
        String sql = """
            UPDATE urls
            SET title = ?
            WHERE base_url = ?
            """;

        try (var connection = ds.getConnection();
             var preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setString(1, titleDoc);
            preparedStatement.setString(2, url);

            int updatedRows = preparedStatement.executeUpdate();

            if (updatedRows == 0) {
                throw new IllegalArgumentException(
                        "URL не найден: " + url
                );
            }

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Ошибка при сохранении title для URL: " + url,
                    e
            );
        }
    }

    public record UrlRow(long id, String baseUrl) {
    }
}
