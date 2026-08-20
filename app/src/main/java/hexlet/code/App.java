package hexlet.code;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.resolve.ResourceCodeResolver;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.rendering.template.JavalinJte;
import static io.javalin.apibuilder.ApiBuilder.*;
import static java.util.Objects.requireNonNull;

//import com.squareup.okhttp.MediaType;
//import okhttp3.OkHttpMediaType;
import okhttp3.MediaType;
//import com.squareup.okhttp.OkHttpClient;
//import com.squareup.okhttp.Request;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

//import com.squareup.okhttp.RequestBody;
import okhttp3.RequestBody;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;

import javax.sql.DataSource;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
public class App {
    private static TemplateEngine createTemplateEngine() {
        ClassLoader classLoader = App.class.getClassLoader();
        ResourceCodeResolver codeResolver = new ResourceCodeResolver("templates", classLoader);
        TemplateEngine templateEngine = TemplateEngine.create(codeResolver, ContentType.Html);
        return templateEngine;
    }
    private static Javalin appInstance;
    private static final String FLASH_KEY = "flash";

    // подстройте при необходимости
    static final int HTTP_PORT = 8080;

    private static String toBaseUrl(String input) throws Exception {
        URL parsed = new URI(input).toURL(); // конструктор URL не используем напрямую
        String protocol = parsed.getProtocol();
        String host = parsed.getHost();
        int port = parsed.getPort(); // -1 если не указан

        if (port == -1) {
            return protocol + "://" + host;
        }
        return protocol + "://" + host + ":" + port;
    }
    private static DataSource buildDataSource() {
        // TODO: например, HikariCP:
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl("jdbc:postgresql://...");
        //cfg.setUsername(...); cfg.setPassword(...);
        return new HikariDataSource(cfg);
        //throw new UnsupportedOperationException("Implement DataSource creation");
    }
    public List<UrlRow> findAll() {
        String sql = "SELECT id, base_url FROM urls ORDER BY id";

        try (var c = ds.getConnection();
             var ps = c.prepareStatement(sql);
             var rs = ps.executeQuery()) {

            List<UrlRow> out = new ArrayList<>();
            while (rs.next()) {
                out.add(new UrlRow(rs.getLong("id"), rs.getString("base_url")));
            }
            return out;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    public Optional<UrlRepositoryJdbc.UrlRow> findById(long id) {
        String sql = "SELECT id, base_url FROM urls WHERE id = ?";

        try (var c = ds.getConnection();
             var ps = c.prepareStatement(sql)) {

            ps.setLong(1, id);

            try (var rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(new UrlRepositoryJdbc.UrlRow(
                        rs.getLong("id"),
                        rs.getString("base_url")
                ));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    private static void insertH1(DataSource ds, String url, String h1Text) throws SQLException {
        String sql = "insert into page_h1 (url, h1_text) values (?, ?)";
        try (Connection con = ds.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, url);
            ps.setString(2, h1Text);
            ps.executeUpdate();
        }
    }
    private static void insertDescription(DataSource ds, String url, String content) throws SQLException {
        String sql = "insert into page_meta_description (url, content) values (?, ?)";
        try (Connection con = ds.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, url);
            ps.setString(2, content);
            ps.executeUpdate();
        }
    }
    public static Javalin getApp() {
        if (appInstance != null) return appInstance;
        DataSource ds = buildDataSource();
        DataSource dsf = DataSourceFactory.create();
        DataSourceFactory.initSchema(dsf);
        var repo = new UrlRepositoryJdbc(ds);

        appInstance = Javalin.create(config -> {
            //config.app4567Port(7000); // при желании замените/уберите
            config.jetty.port = 7000;
            config.enableCorsForAllOrigins();
            config.bundledPlugins.enableDevLogging();
            config.fileRenderer(new JavalinJte(createTemplateEngine()));
            config.routes.apiBuilder(() -> {
                get("/", ctx -> ctx.result("Hello World"));
                get("/health", ctx -> ctx.json(new HealthResponse(true)));
                post("/users", ctx -> ctx.result("POST /users"));
                get("/", ctx -> {
                    String flash = ctx.sessionAttribute(FLASH_KEY);
                    if (flash != null) ctx.sessionAttribute(FLASH_KEY, null);
                    ctx.render("index", Map.of("flash", flash));
                });
                post("/urls", ctx -> {
                    String input = ctx.formParam("url");

                    try {
                        requireNonNull(input, "Missing url");

                        String baseUrl = toBaseUrl(input); // бросает exception если некорректно

                        var result = repo.upsertLikeCheck(baseUrl);

                        if (!result.insertedNew()) {
                            ctx.sessionAttribute(FLASH_KEY, "Страница уже существует");
                            ctx.redirect("/urls/" + result.row().id());
                        } else {
                            ctx.sessionAttribute(FLASH_KEY, "Страница успешно добавлена");
                            ctx.redirect("/urls/" + result.row().id());
                        }
                    } catch (Exception e) {
                        // Некорректный URL
                        ctx.status(422);
                        ctx.render("index", Map.of("flash", "Некорректный URL"));
                    }
                });
                get("/urls/{id}", ctx -> {
                    long id = Long.parseLong(ctx.pathParam("id"));

                    String flash = ctx.sessionAttribute(FLASH_KEY);
                    if (flash != null) ctx.sessionAttribute(FLASH_KEY, null);

                    var row = repo.findById(id)
                            .orElseThrow(() -> new IllegalArgumentException("Not found"));

                    ctx.render("url", Map.of(
                            "url", row,
                            "flash", flash
                    ));
                });
                get("/urls", ctx -> {
                    String flash = ctx.sessionAttribute(FLASH_KEY);
                    if (flash != null) ctx.sessionAttribute(FLASH_KEY, null);

                    var all = findAll();
                    ctx.render("urls", Map.of(
                            "urls", all,
                            "flash", flash
                    ));
                });
                get("/urls/{id}", ctx -> {
                    long id = Long.parseLong(ctx.pathParam("id"));

                    String flash = ctx.sessionAttribute("flash");
                    if (flash != null) ctx.sessionAttribute("flash", null);

                    var row = findById(id).orElseThrow(() -> new IllegalArgumentException("Not found"));

                    ctx.render("url", Map.of(
                            "url", row,
                            "flash", flash
                    ));
                });

            });
        });//.routes(() -> {
        //    get("/", ctx -> ctx.result("Hello World"));

            // пример: healthcheck
        //    get("/health", ctx -> ctx.json(new HealthResponse(true)));
        //});
        //appInstance = Javalin.create(config -> {
        //    config.bundledPlugins.enableDevLogging();
        //});
        //appInstance.get("/", ctx -> ctx.result("Hello World"));
        //appInstance.start(7070);

        // --- JDBC setup (Hikari) ---
        DataSource ds = JdbcUtil.createDataSource();

        // (Опционально) seed url для демонстрации
        JdbcUtil.ensureSeedUrlExists(ds, 1L, "https://example.com");

        // --- MockWebServer ---
        MockWebServer mock = new MockWebServer();
        mock.start(); // выбирает порт сам
        String mockBaseUrl = mock.url("/").toString(); // типа http://localhost:xxxxx/

        // Поставим “ответ” на запрос создания checks
        mock.enqueue(new MockResponse()
                .setResponseCode(201)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"checkId\": 999, \"status\":\"CREATED\"}"));

        // --- HTTP server (Jetty) ---
        Server server = new Server(HTTP_PORT);
        ServletContextHandler context = new ServletContextHandler(server, "/");

        // Передаём зависимости в сервлет
        var handler = new UrlsChecksServlet(ds, new OkHttpClient(), mockBaseUrl);
        context.addServlet(new ServletHolder(handler), "/*");

        server.start();
        server.join();

        String body = Unirest.get("http://localhost")
                .header("Accept", "text/html")
                .asString()
                .getBody();
        Document doc = Jsoup.parse(body);
        String title = doc.title();
        String outTitle = (title == null || title.isBlank()) ? null : title;

        appInstance.get("/fetch-title", ctx -> {
            String url = ctx.queryParam("url");
            if (url == null || url.isBlank()) {
                ctx.status(400).result("Missing query param: url");
                return;
            }

            try {
                String title = PageTitleFetcher.fetchTitle(url);

                if (title == null) {
                    ctx.status(204); // нет <title>
                    return;
                }

                repo.insertTitle(url, title);
                ctx.json(java.util.Map.of("url", url, "title", title));
            } catch (Exception e) {
                ctx.status(500).result("Error: " + e.getMessage());
            }
        });
        Document doc = Jsoup.connect(url).get();
        boolean hasH1 = !doc.select("h1").isEmpty();
        appInstance.post("/save-h1", ctx -> {
            String url = ctx.formParam("url"); // или ctx.body()
            if (url == null || url.isBlank()) {
                ctx.status(400).result("Missing url");
                return;
            }

            Document doc = Jsoup.connect(url).get();
            Element h1 = doc.selectFirst("h1");

            if (h1 == null) {
                ctx.status(204); // нет контента
                return;
            }

            String h1Text = h1.text();

            insertH1(ds, url, h1Text);

            ctx.status(200).result("Saved h1: " + h1Text);
        });
        Element meta = doc.selectFirst("meta[name=description]");
        boolean hasDescription = meta != null && meta.hasAttr("content") && !meta.attr("content").isBlank();
        String description = (meta != null) ? meta.attr("content") : null;
        boolean hasDescription = doc.select("meta[name=description][content]").size() > 0;
        appInstance.post("/save-description", ctx -> {
            String url = ctx.formParam("url"); // или ctx.bodyParam("url"), как удобнее
            if (url == null || url.isBlank()) {
                ctx.status(400).result("Missing url");
                return;
            }

            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0")
                    .timeout(10000)
                    .get();

            Element meta = doc.selectFirst("meta[name=description][content]");
            if (meta == null) {
                ctx.status(204).result("No meta description found");
                return;
            }

            String content = meta.attr("content").trim();
            if (content.isEmpty()) {
                ctx.status(204).result("Meta description content is empty");
                return;
            }

            insertDescription(ds, url, content);
            ctx.status(200).result("Saved meta description");
        });

        app.get("/check-site", ctx -> {
            String url = ctx.queryParam("url");
            if (url == null || url.isBlank()) {
                ctx.status(400).result("Missing url");
                return;
            }

            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0")
                    .timeout(10000)
                    .get();

            List<String> lines = new ArrayList<>();

            // 1) h1
            Element h1 = doc.selectFirst("h1");
            if (h1 != null) {
                lines.add("1) <h1>: найден — \"" + h1.text().trim() + "\"");
                // сюда же можно вставлять в БД
            } else {
                lines.add("1) <h1>: не найден");
            }

            // 2) meta description
            Element metaDesc = doc.selectFirst("meta[name=description][content]");
            if (metaDesc != null) {
                String content = metaDesc.attr("content").trim();
                if (!content.isEmpty()) {
                    lines.add("2) meta description: найден — content=\"" + content + "\"");
                    // сюда же можно вставлять в БД
                } else {
                    lines.add("2) meta description: найден, но content пустой");
                }
            } else {
                lines.add("2) meta description: не найден");
            }

            // вывод списком (plain text)
            ctx.contentType("text/plain; charset=utf-8");
            ctx.result(String.join("\n", lines));
        });
        return appInstance;
    }

    private record HealthResponse(boolean ok) {}

    // пример запуска:
    public static void main(String[] args) {
        App.getApp().start();
    }
}
