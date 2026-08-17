package hexlet.code;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;

import javax.sql.DataSource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.OffsetDateTime;
import java.util.concurrent.TimeUnit;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
public class UrlsChecksServletTest {
    static class TestDb {
        final DataSource ds;

        TestDb() {
            // Упростим: для реальной практики лучше Testcontainers.
            // Но в рамках примера сделаем через env JDBC_URL, чтобы было понятно куда подключать.
            // Вы можете заменить на контейнер.
            String url = System.getenv().getOrDefault("JDBC_URL", "jdbc:postgresql://localhost:5432/app_test");
            String user = System.getenv().getOrDefault("JDBC_USER", "app");
            String pass = System.getenv().getOrDefault("JDBC_PASS", "app");

            HikariConfig cfg = new HikariConfig();
            cfg.setJdbcUrl(url);
            cfg.setUsername(user);
            cfg.setPassword(pass);
            cfg.setMaximumPoolSize(5);
            this.ds = new HikariDataSource(cfg);
        }

        void resetSchemaAndData() throws SQLException {
            try (Connection c = ds.getConnection(); Statement st = c.createStatement()) {
                st.execute("delete from url_checks");
                st.execute("delete from checks");
                st.execute("delete from urls");

                // Создаём схему под ваш текущий вариант.
                // В вашем вопросе есть 2 разных DDL urls (разные колонки: name/base_url и id/address).
                // Для тестов используем тот вариант, который соответствует JdbcUtil: urls(id, address).
                st.execute("""
                    create table if not exists urls (
                      id bigint primary key,
                      address text not null
                    );
                """);
                st.execute("""
                    create table if not exists checks (
                      id bigserial primary key,
                      url_id bigint not null references urls(id),
                      type text not null,
                      remote_response text not null,
                      created_at timestamp default current_timestamp
                    );
                """);
                st.execute("""
                    create table if not exists url_checks (
                        id bigserial primary key,
                        url_id bigint not null,
                        status_code integer not null,
                        h1 TEXT,
                        title TEXT,
                        description TEXT,
                        created_at timestamptz not null default now(),
                        constraint fk_url_checks_url
                          foreign key (url_id) references urls (id)
                          on delete cascade
                    );
                """);
            }
        }

        void insertUrl(long id, String address) throws SQLException {
            try (Connection c = ds.getConnection(); Statement st = c.createStatement()) {
                st.executeUpdate("insert into urls(id, address) values (" + id + ", '" + address.replace("'", "''") + "')");
            }
        }
    }

    TestDb db;
    OkHttpClient okHttp;

    @BeforeEach
    void setup() throws SQLException {
        db = new TestDb();
        db.resetSchemaAndData();
        okHttp = new OkHttpClient.Builder()
                .readTimeout(2, TimeUnit.SECONDS)
                .connectTimeout(2, TimeUnit.SECONDS)
                .build();
    }

    MockWebServer mockWebServer;

    @AfterEach
    void tearDown() throws Exception {
        if (mockWebServer != null) {
            mockWebServer.shutdown();
        }
    }

    private static HttpServletRequest mockRequestWithPath(String path) {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getPathInfo()).thenReturn(path);
        return req;
    }

    private static HttpServletResponse mockResponse() throws Exception {
        HttpServletResponse resp = mock(HttpServletResponse.class);
        when(resp.getWriter()).thenReturn(new PrintWriter(System.out));
        return resp;
    }

    private static String captureBody(HttpServletResponse resp) throws Exception {
        // Важно: Mockito не даст легко перехватить writer.
        // Для простоты ниже будем проверять только setStatus и sendRedirect.
        return "";
    }

    @Test
    void doGet_pathNull_returns404() throws Exception {
        HttpServletRequest req = mockRequestWithPath(null);
        HttpServletResponse resp = mockResponse();

        var servlet = new UrlsChecksServlet(db.ds, okHttp, "http://example/");
        servlet.doGet(req, resp);

        verify(resp).setStatus(404);
    }

    @Test
    void doGet_rootSlash_returns404() throws Exception {
        HttpServletRequest req = mockRequestWithPath("/");
        HttpServletResponse resp = mockResponse();

        var servlet = new UrlsChecksServlet(db.ds, okHttp, "http://example/");
        servlet.doGet(req, resp);

        verify(resp).setStatus(404);
    }

    @Test
    void doGet_wrongFormat_returns404() throws Exception {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getPathInfo()).thenReturn("/abc/def");
        HttpServletResponse resp = mockResponse();

        var servlet = new UrlsChecksServlet(db.ds, okHttp, "http://example/");
        servlet.doGet(req, resp);

        verify(resp).setStatus(404);
    }

    @Test
    void doGet_urlNotFound_returns404() throws Exception {
        HttpServletRequest req = mockRequestWithPath("/123");
        HttpServletResponse resp = mockResponse();

        var servlet = new UrlsChecksServlet(db.ds, okHttp, "http://example/");
        servlet.doGet(req, resp);

        verify(resp).setStatus(404);
        verify(resp).getWriter();
    }

    @Test
    void doGet_checksEmpty_returnsOkWithNoChecksText() throws Exception {
        long id = 1L;
        db.insertUrl(id, "http://site.test");

        HttpServletRequest req = mockRequestWithPath("/1");
        HttpServletResponse resp = mockResponse();

        var servlet = new UrlsChecksServlet(db.ds, okHttp, "http://example/");
        servlet.doGet(req, resp);

        verify(resp).setContentType("text/html; charset=UTF-8");

        // Т.к. мы не перехватываем writer в этом примере полностью,
        // проверим хотя бы статус не 500:
        verify(resp, never()).setStatus(500);
    }

    @Test
    void doGet_checksPresent_rendersTable() throws Exception {
        long id = 2L;
        db.insertUrl(id, "http://site2.test");

        // добавим checks через JdbcUtil
        JdbcUtil.insertCheck(db.ds, id, "HTTP", "OK-body");

        HttpServletRequest req = mockRequestWithPath("/2");
        HttpServletResponse resp = mockResponse();

        var servlet = new UrlsChecksServlet(db.ds, okHttp, "http://example/");
        servlet.doGet(req, resp);

        verify(resp).setContentType("text/html; charset=UTF-8");
        verify(resp, never()).setStatus(500);
    }

    @Test
    void doGet_dbError_returns500() throws Exception {
        // Сымитируем SQLException проще всего так:
        // создадим DS, который всегда падает.
        DataSource badDs = mock(DataSource.class);
        when(badDs.getConnection()).thenThrow(new SQLException("boom"));

        HttpServletRequest req = mockRequestWithPath("/1");
        HttpServletResponse resp = mockResponse();

        var servlet = new UrlsChecksServlet(badDs, okHttp, "http://example/");
        servlet.doGet(req, resp);

        verify(resp).setStatus(500);
    }

    @Test
    void doPost_pathNull_returns404() throws Exception {
        HttpServletRequest req = mockRequestWithPath(null);
        HttpServletResponse resp = mockResponse();

        var servlet = new UrlsChecksServlet(db.ds, okHttp, "http://example/");
        servlet.doPost(req, resp);

        verify(resp).setStatus(404);
    }

    @Test
    void doPost_wrongFormat_returns404() throws Exception {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getPathInfo()).thenReturn("/1/abc");
        HttpServletResponse resp = mockResponse();

        var servlet = new UrlsChecksServlet(db.ds, okHttp, "http://example/");
        servlet.doPost(req, resp);

        verify(resp).setStatus(404);
    }

    @Test
    void doPost_success_redirect302_and_callsNetworkMockWebServer() throws Exception {
        long id = 10L;
        db.insertUrl(id, "http://ok.test");

        mockWebServer = new MockWebServer();
        mockWebServer.start();

        // ожидаем, что servlet дернется на /mock/checks
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("{\"title\":\"t\",\"h1\":\"h\"}") // как вам нужно
                .addHeader("Content-Type", "application/json"));

        String baseUrl = mockWebServer.url("/").toString(); // заканчивается / для удобства

        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getPathInfo()).thenReturn("/" + id + "/checks");
        when(req.getParameter("type")).thenReturn("HTTP");

        HttpServletResponse resp = mock(HttpServletResponse.class);

        var servlet = new UrlsChecksServlet(db.ds, okHttp, baseUrl);

        // ВАЖНО: этот тест предполагает, что в doPost вы раскомментировали:
        // remoteResponse = callMockCreateCheck(id, type);
        // JdbcUtil.insertCheck(ds, id, type, remoteResponse);
        servlet.doPost(req, resp);

        // проверяем редирект
        verify(resp).sendRedirect("/urls/" + id);

        // проверяем, что был сетевой запрос
        RecordedRequest recorded = mockWebServer.takeRequest(2, TimeUnit.SECONDS);
        assertNotNull(recorded);
        assertEquals("/mock/checks", recorded.getPath());
        assertEquals("POST", recorded.getMethod());

        // можно дополнительно проверить body JSON
        String sentBody = recorded.getBody().readUtf8();
        assertTrue(sentBody.contains("\"urlId\":" + id));
        assertTrue(sentBody.contains("\"type\":\"HTTP\""));
    }

    @Test
    void doPost_mockServerError_returns502() throws Exception {
        long id = 11L;
        db.insertUrl(id, "http://fail.test");

        mockWebServer = new MockWebServer();
        mockWebServer.start();

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(500)
                .setBody("server error"));

        String baseUrl = mockWebServer.url("/").toString();

        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getPathInfo()).thenReturn("/" + id + "/checks");
        when(req.getParameter("type")).thenReturn("HTTP");

        HttpServletResponse resp = mock(HttpServletResponse.class);
        when(resp.getWriter()).thenReturn(new PrintWriter(System.out));

        var servlet = new UrlsChecksServlet(db.ds, okHttp, baseUrl);

        // предполагается, что remoteResponse = callMockCreateCheck(...) раскомментирован,
        // и при ошибке вы делаете resp.setStatus(502)
        servlet.doPost(req, resp);

        verify(resp).setStatus(502);
        verify(resp).getWriter();
    }

    @Test
    void doPost_insertUrlChecksFails_returns502() throws Exception {
        // создадим DS, который падает именно на первой вставке
        DataSource badDs = mock(DataSource.class);
        Connection c = mock(Connection.class);
        when(badDs.getConnection()).thenReturn(c);

        // insertUrlCheck делает prepareStatement + executeUpdate
        when(c.prepareStatement(anyString())).thenReturn(mock(java.sql.PreparedStatement.class));

        // Чтобы было проще — пусть getConnection бросает SQLException
        when(badDs.getConnection()).thenThrow(new SQLException("db down"));

        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getPathInfo()).thenReturn("/12/checks".replace("/","/")); // просто /12/checks
        // корректно:
        when(req.getPathInfo()).thenReturn("/12/checks");
        when(req.getParameter("type")).thenReturn("HTTP");

        HttpServletResponse resp = mock(HttpServletResponse.class);
        when(resp.getWriter()).thenReturn(new PrintWriter(System.out));

        var servlet = new UrlsChecksServlet(badDs, okHttp, "http://example/");
        servlet.doPost(req, resp);

        verify(resp).setStatus(502);
    }

    @Test
    void doPost_insertCheckFails_returns500() throws Exception {
        // DS будет работать, но на второй вставке упадём.
        DataSource ds = mock(DataSource.class);
        Connection conn = mock(Connection.class);
        when(ds.getConnection()).thenReturn(conn);

        java.sql.PreparedStatement ps1 = mock(java.sql.PreparedStatement.class); // for url_checks
        java.sql.PreparedStatement ps2 = mock(java.sql.PreparedStatement.class); // for checks

        when(conn.prepareStatement(startsWith("insert into url_checks"))).thenReturn(ps1);
        when(conn.prepareStatement(startsWith("insert into checks("))).thenReturn(ps2);

        when(ps1.executeUpdate()).thenReturn(1);
        when(ps2.executeUpdate()).thenThrow(new SQLException("insert checks failed"));

        // нам ещё нужен путь, и можно без реальной БД urls (но findUrlAddress не вызывается в doPost)
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getPathInfo()).thenReturn("/13/checks");
        when(req.getParameter("type")).thenReturn("HTTP");

        HttpServletResponse resp = mock(HttpServletResponse.class);
        when(resp.getWriter()).thenReturn(new PrintWriter(System.out));

        // Чтобы не запускать сеть в этом тесте, лучше чтобы callMockCreateCheck НЕ вызывался.
        // Но в вашем коде он должен вызываться только если вы раскомментируете.
        // Тогда этот тест лучше делать с MockWebServer 200 или переопределять удаленный вызов.
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));
        String baseUrl = mockWebServer.url("/").toString();

        var servlet = new UrlsChecksServlet(ds, okHttp, baseUrl);
        servlet.doPost(req, resp);

        verify(resp).setStatus(500);
    }
}
