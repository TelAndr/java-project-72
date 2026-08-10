package hexlet.code;
import okhttp3.*;
import org.jsoup.Jsoup;
import org.jsoup.helper.HttpConnection;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.TestInstance;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AppIntegrationTest {
    private static final OkHttpClient http = new OkHttpClient();

    private static int port = 7001; // не 7000, чтобы не конфликтовать
    private String baseUrl;

    @BeforeAll
    void startServer() throws Exception {
        // H2 для тестов
        System.setProperty("db.jdbcUrl", "jdbc:h2:mem:testdb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        System.setProperty("db.user", "sa");
        System.setProperty("db.password", "");

        // Если schema создаётся через DataSourceFactory.initSchema(...)
        // важно чтобы она создала таблицы в этой же H2.
        // Для этого DataSourceFactory тоже должен использовать эти свойства/профиль.

        // Запуск Javalin
        // В вашем App порт зашит в 7000. Для тестов лучше дать возможность менять порт.
        // Если менять порт невозможно — используйте 7000, но тогда позаботьтесь о конфликте.
        // Здесь допустим минимальный патч: config.jetty.port = Integer.getInteger("app.port", 7000);
        baseUrl = "http://localhost:" + port;

        Thread t = new Thread(() -> {
            try {
                // Вынесите/поддержите параметр app.port в App.
                // Если сейчас нельзя — скажите, я адаптирую под ваш текущий код.
                App.getApp().start();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        t.setDaemon(true);
        t.start();

        // дождаться, пока сервер поднимется
        awaitServerUp();
    }

    private void awaitServerUp() throws InterruptedException {
        for (int i = 0; i < 50; i++) {
            try {
                Request req = new Request.Builder().url(baseUrl + "/health").get().build();
                try (Response resp = http.newCall(req).execute()) {
                    if (resp.code() == 200) return;
                }
            } catch (IOException ignored) {}
            Thread.sleep(100);
        }
        fail("Server did not start");
    }

    private Response exec(Request request) throws IOException {
        return http.newCall(request).execute();
    }

    @Test
    void health_get_returnsOkJson() throws IOException {
        Request req = new Request.Builder().url(baseUrl + "/health").get().build();
        try (Response resp = exec(req)) {
            assertThat(resp.code()).isEqualTo(200);
            String body = resp.body().string();
            assertThat(body).contains("\"ok\"");
        }
    }

    @Test
    void root_get_rendersIndex() throws IOException {
        Request req = new Request.Builder().url(baseUrl + "/").get().build();
        try (Response resp = exec(req)) {
            assertThat(resp.code()).isEqualTo(200);
            String html = resp.body().string();
            assertThat(html).contains("name=\"url\""); // индикатор, что это index (зависит от шаблона)
        }
    }

    @Test
    void users_post_ok() throws IOException {
        RequestBody form = new FormBody.Builder().add("a", "b").build();
        Request req = new Request.Builder().url(baseUrl + "/users").post(form).build();
        try (Response resp = exec(req)) {
            assertThat(resp.code()).isEqualTo(200);
            assertThat(resp.body().string()).contains("POST /users");
        }
    }

    @Test
    void urls_get_returnsUrlsPage() throws IOException {
        Request req = new Request.Builder().url(baseUrl + "/urls").get().build();
        try (Response resp = exec(req)) {
            assertThat(resp.code()).isEqualTo(200);
            String html = resp.body().string();
            assertThat(html).contains("data-test"); // зависит от шаблона urls
        }
    }

    @Test
    void postUrls_invalidUrl_returns422() throws IOException {
        FormBody form = new FormBody.Builder().add("url", "not-a-url").build();
        Request req = new Request.Builder().url(baseUrl + "/urls").post(form).build();
        try (Response resp = exec(req)) {
            assertThat(resp.code()).isEqualTo(422);
        }
    }

    @Test
    void postUrls_insertsOrUpsertsAndPageOpens() throws IOException {
        // 1) Вставка нового
        String input = "https://example.com/path";
        FormBody form = new FormBody.Builder().add("url", input).build();
        Request req = new Request.Builder().url(baseUrl + "/urls").post(form).build();

        String location;
        try (Response resp = exec(req)) {
            assertThat(resp.code()).isBetween(300, 399);
            location = resp.header("Location");
        }

        assertThat(location).startsWith("/urls/");
        long id = extractId(location);

        try (var c = TestDb.connect();
             var ps = c.prepareStatement("SELECT COUNT(*) FROM urls WHERE id = ?")) {
            ps.setLong(1, id);
            var rs = ps.executeQuery();
            rs.next();
            assertThat(rs.getInt(1)).isEqualTo(1);
        }

        // 2) Страница /urls/{id} открывается и содержит требуемую разметку checks
        String pageHtml;
        Request get = new Request.Builder().url(baseUrl + "/urls/" + id).get().build();
        try (Response resp = exec(get)) {
            assertThat(resp.code()).isEqualTo(200);
            pageHtml = resp.body().string();
        }

        Document doc = Jsoup.parse(pageHtml);

        // Форма проверки с ключевой разметкой
        assertThat(doc.select("form[method=post][action=/urls/" + id + "/checks]").size()).isGreaterThanOrEqualTo(1);
        assertThat(doc.select("table[data-test=checks] thead th").eachText())
                .contains("ID", "Код ответа", "h1", "title", "description", "Дата создания");

        // 3) Повторная вставка того же URL (должно не создать новую запись, но открыть ту же страницу)
        Request req2 = new Request.Builder().url(baseUrl + "/urls").post(form).build();
        String location2;
        try (Response resp2 = exec(req2)) {
            assertThat(resp2.code()).isBetween(300, 399);
            location2 = resp2.header("Location");
        }

        long id2 = extractId(location2);
        assertThat(id2).isEqualTo(id);
        try (var c = TestDb.connect();
             var ps = c.prepareStatement("SELECT COUNT(*) FROM urls WHERE id = ?")) {
            ps.setLong(1, id2);
            try (var rs = ps.executeQuery()) {
                rs.next();
                assertThat(rs.getLong(1)).isEqualTo(1L);
            }
        }


        // Страница открывается снова
        HttpConnection.Request get2 = new Request.Builder().url(baseUrl + "/urls/" + id2).get().build();
        try (HttpConnection.Response resp2 = exec(get2)) {
            assertThat(resp2.code()).isEqualTo(200);
            assertThat(resp2.body().string()).contains("table"); // мягкая проверка, т.к. шаблон может меняться
        }
    }

    private long extractId(String location) {
        Matcher m = Pattern.compile("/urls/(\\d+)").matcher(location);
        assertThat(m.find()).isTrue();
        return Long.parseLong(m.group(1));
    }
}
