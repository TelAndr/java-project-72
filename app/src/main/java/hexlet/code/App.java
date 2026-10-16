package hexlet.code;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.resolve.ResourceCodeResolver;
import io.javalin.Javalin;
//import io.javalin.http.Context;
import io.javalin.rendering.template.JavalinJte;
import io.javalin.plugin.bundled.CorsPluginConfig;
import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.post;
import static java.util.Objects.requireNonNull;

//import com.squareup.okhttp.MediaType;
//import okhttp3.OkHttpMediaType;
import kong.unirest.Unirest;
//import okhttp3.MediaType;
//import com.squareup.okhttp.OkHttpClient;
//import com.squareup.okhttp.Request;

//import okhttp3.OkHttpClient;
//import okhttp3.Request;
//import okhttp3.Response;

//import com.squareup.okhttp.RequestBody;
//import okhttp3.RequestBody;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
//import org.eclipse.jetty.server.HandlerContainer;
//import org.eclipse.jetty.server.Server;
//import org.eclipse.jetty.servlet.ServletContextHandler;
//import org.eclipse.jetty.servlet.ServletHolder;
import org.jsoup.Jsoup;

import javax.sql.DataSource;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

//import java.io.IOException;
import java.sql.SQLException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
//import java.util.Optional;
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
    public static Javalin getApp() throws Exception {
        final int numAppPort = 7000;
        if (appInstance != null) {
            return appInstance;
        }
        DataSource ds = JdbcUtil.createDataSource();
        JdbcUtil.ensureSeedUrlExists(ds, 1L, "https://example.com");
        var repo = new UrlRepositoryJdbc(ds);

        appInstance = Javalin.create(config -> {
            final int numBadRequest = 400;
            final int numNoContent = 204;
            final int numInternalServerError = 500;
            final int numOkReq = 200;
            final int numTimeout = 10000;
            final int numUnprocessableContent = 422;
            config.jetty.port = Integer.getInteger("app.port", numAppPort);
            config.bundledPlugins.enableCors(cors ->
                    cors.addRule(CorsPluginConfig.CorsRule::anyHost)
            );

            config.bundledPlugins.enableDevLogging();
            config.fileRenderer(new JavalinJte(createTemplateEngine()));
            config.routes.apiBuilder(() -> {
                get("/", ctx -> ctx.result("Hello World"));
                get("/health", ctx -> ctx.json(new HealthResponse(true)));
                post("/users", ctx -> ctx.result("POST /users"));
                get("/", ctx -> {
                    String flash = ctx.sessionAttribute(FLASH_KEY);
                    if (flash != null) {
                        ctx.sessionAttribute(FLASH_KEY, null);
                    }
                    ctx.render("index", Map.of("flash", flash));
                });
                get("/fetch-title", ctx -> {
                    String url = ctx.queryParam("url");
                    if (url == null || url.isBlank()) {
                        ctx.status(numBadRequest).result("Missing query param: url");
                        return;
                    }

                    try {
                        String titleDoc = PageTitleFetcher.fetchTitle(url);

                        if (titleDoc == null) {
                            ctx.status(numNoContent); // нет <title>
                            return;
                        }

                        repo.insertTitle(url, titleDoc);
                        ctx.json(java.util.Map.of("url", url, "title", titleDoc));
                    } catch (Exception e) {
                        ctx.status(numInternalServerError).result("Error: " + e.getMessage());
                    }
                });
                post("/save-h1", ctx -> {
                    String url = ctx.formParam("url"); // или ctx.body()
                    String inpUrl = ctx.queryParam("url");
                    Document docJsUrl = Jsoup.connect(inpUrl).get();
                    boolean hasH1 = !docJsUrl.select("h1").isEmpty();
                    if (url == null || url.isBlank()) {
                        ctx.status(numBadRequest).result("Missing url");
                        return;
                    }

                    Document docJsUrlNew = Jsoup.connect(url).get();
                    Element h1 = docJsUrlNew.selectFirst("h1");

                    if (h1 == null) {
                        ctx.status(numNoContent); // нет контента
                        return;
                    }

                    String h1Text = h1.text();

                    insertH1(ds, url, h1Text);

                    ctx.status(numOkReq).result("Saved h1: " + h1Text);
                });
                post("/save-description", ctx -> {
                    String url = ctx.formParam("url"); // или ctx.bodyParam("url"), как удобнее
                    if (url == null || url.isBlank()) {
                        ctx.status(numBadRequest).result("Missing url");
                        return;
                    }

                    Document docJsoup = Jsoup.connect(url)
                            .userAgent("Mozilla/5.0")
                            .timeout(numTimeout)
                            .get();

                    Element metaInf = docJsoup.selectFirst("meta[name=description][content]");
                    if (metaInf == null) {
                        ctx.status(numNoContent).result("No meta description found");
                        return;
                    }

                    String content = metaInf.attr("content").trim();
                    if (content.isEmpty()) {
                        ctx.status(numNoContent).result("Meta description content is empty");
                        return;
                    }

                    insertDescription(ds, url, content);
                    ctx.status(numOkReq).result("Saved meta description");
                });
                get("/check-site", ctx -> {
                    String url = ctx.queryParam("url");
                    if (url == null || url.isBlank()) {
                        ctx.status(numBadRequest).result("Missing url");
                        return;
                    }

                    Document docJc = Jsoup.connect(url)
                            .userAgent("Mozilla/5.0")
                            .timeout(numTimeout)
                            .get();

                    List<String> lines = new ArrayList<>();

                    // 1) h1
                    Element h1 = docJc.selectFirst("h1");
                    if (h1 != null) {
                        lines.add("1) <h1>: найден — \"" + h1.text().trim() + "\"");
                        // сюда же можно вставлять в БД
                    } else {
                        lines.add("1) <h1>: не найден");
                    }

                    // 2) meta description
                    Element metaDesc = docJc.selectFirst("meta[name=description][content]");
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
                post("/urls", ctx -> {
                    String input = ctx.formParam("url");

                    try {
                        requireNonNull(input, "Missing url");

                        String baseUrl = toBaseUrl(input); // бросает exception если некорректно

                        var result = repo.upsertLikeCheck(baseUrl);

                        if (!result.insertedNew()) {
                            ctx.sessionAttribute(FLASH_KEY, "Страница уже существует");
                            ctx.redirect("/urls/" + result.getId()); // row().id()
                        } else {
                            ctx.sessionAttribute(FLASH_KEY, "Страница успешно добавлена");
                            ctx.redirect("/urls/" + result.getId()); // row().id()
                        }
                    } catch (Exception e) {
                        // Некорректный URL
                        ctx.status(numUnprocessableContent);
                        ctx.render("index", Map.of("flash", "Некорректный URL"));
                    }
                });
                get("/urls/{id}", ctx -> {
                    long id = Long.parseLong(ctx.pathParam("id"));

                    String flash = ctx.sessionAttribute(FLASH_KEY);
                    if (flash != null) {
                        ctx.sessionAttribute(FLASH_KEY, null);
                    }

                    var row = repo.findById(id)
                            .orElseThrow(() -> new IllegalArgumentException("Not found"));

                    ctx.render("url", Map.of(
                            "url", row,
                            "flash", flash
                    ));
                });
                get("/urls", ctx -> {
                    String flash = ctx.sessionAttribute(FLASH_KEY);
                    if (flash != null) {
                        ctx.sessionAttribute(FLASH_KEY, null);
                    }

                    UrlRepositoryJdbc urlRepJDBC = new UrlRepositoryJdbc(ds);
                    var all = urlRepJDBC.findAll();
                    ctx.render("urls", Map.of(
                            "urls", all,
                            "flash", flash
                    ));
                });
                get("/urls/{id}", ctx -> {
                    long id = Long.parseLong(ctx.pathParam("id"));

                    String flash = ctx.sessionAttribute("flash");
                    if (flash != null) {
                        ctx.sessionAttribute("flash", null);
                    }

                    var row = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Not found"));

                    ctx.render("url", Map.of(
                            "url", row,
                            "flash", flash
                    ));
                });

            });
        });

        // --- JDBC setup (Hikari) ---
        //ds = JdbcUtil.createDataSource();

        // (Опционально) seed url для демонстрации
        JdbcUtil.ensureSeedUrlExists(ds, 1L, "https://example.com");

        // --- MockWebServer ---
        MockWebServer mock = new MockWebServer();
        mock.start(); // выбирает порт сам
        String mockBaseUrl = mock.url("/").toString(); // типа http://localhost:xxxxx/

        // Поставим “ответ” на запрос создания checks
        final int numCreated = 201;
        mock.enqueue(new MockResponse()
                .setResponseCode(numCreated)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"checkId\": 999, \"status\":\"CREATED\"}"));

        String body = Unirest.get("http://localhost")
                .header("Accept", "text/html")
                .asString()
                .getBody();
        Document doc = Jsoup.parse(body);
        String title = doc.title();
        String outTitle = (title == null || title.isBlank()) ? null : title;

        Element meta = doc.selectFirst("meta[name=description]");
        boolean hasDescription = meta != null && meta.hasAttr("content") && !meta.attr("content").isBlank();
        String description = (meta != null) ? meta.attr("content") : null;
        boolean hasDescriptionEl = doc.select("meta[name=description][content]").size() > 0;
        return appInstance;
    }

    private record HealthResponse(boolean ok) {

    }

    // пример запуска:
    public static void main(String[] args) throws Exception {
        App.getApp().start();
    }
}
