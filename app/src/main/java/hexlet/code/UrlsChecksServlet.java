import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.MediaType;

import javax.sql.DataSource;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Map;

public class UrlsChecksServlet extends HttpServlet {

    private final DataSource ds;
    private final OkHttpClient http;
    private final String mockBaseUrl; // например http://localhost:12345/
    private record MockResult(String body, int statusCode) {}

    public UrlsChecksServlet(DataSource ds, OkHttpClient http, String mockBaseUrl) {
        this.ds = ds;
        this.http = http;
        this.mockBaseUrl = mockBaseUrl.endsWith("/") ? mockBaseUrl : mockBaseUrl + "/";
    }

    /*@Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getPathInfo();
        // Jetty может дать null, поэтому нормализуем
        if (path == null || path.equals("/")) {
            resp.setStatus(404);
            resp.getWriter().write("Not found");
            return;
        }

        // ожидаем: /{id}
        String[] parts = path.split("/");
        if (parts.length == 2 && !parts[1].isBlank()) {
            long id = Long.parseLong(parts[1]);
            try {
                var address = JdbcUtil.findUrlAddress(ds, id);
                if (address == null) {
                    resp.setStatus(404);
                    resp.getWriter().write("URL not found");
                    return;
                }

                String html = renderUrlPage(id, address, null);
                resp.setContentType("text/html; charset=UTF-8");
                resp.getWriter().write(html);
            } catch (SQLException e) {
                resp.setStatus(500);
                resp.getWriter().write("DB error: " + e.getMessage());
            }
            return;
        }

        resp.setStatus(404);
        resp.getWriter().write("Not found");
    } */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getPathInfo();
        if (path == null || path.equals("/")) {
            resp.setStatus(404);
            resp.getWriter().write("Not found");
            return;
        }

        // ожидаем: /{id}
        String[] parts = path.split("/");
        if (parts.length == 2 && !parts[1].isBlank()) {
            long id = Long.parseLong(parts[1]);

            try {
                var address = JdbcUtil.findUrlAddress(ds, id);
                if (address == null) {
                    resp.setStatus(404);
                    resp.getWriter().write("URL not found");
                    return;
                }

                // собираем проверки
                StringBuilder rows = new StringBuilder();
                final boolean[] has = {false};

                JdbcUtil.streamChecksForUrl(ds, id, (checkId, type, remoteResponse, createdAt) -> {
                    has[0] = true;
                    rows.append("""
            <tr>
              <td>%d</td>
              <td>%s</td>
              <td>%s</td>
            </tr>
          """.formatted(
                            checkId,
                            escapeHtml(type),
                            createdAt == null ? "" : escapeHtml(createdAt.toString())
                    ));
                });

                String checksHtml;
                if (!has[0]) {
                    checksHtml = "<p>Проверок пока нет.</p>";
                } else {
                    checksHtml = """
            <table border="1" cellpadding="6" cellspacing="0">
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Тип</th>
                  <th>Дата</th>
                </tr>
              </thead>
              <tbody>
                %s
              </tbody>
            </table>
          """.formatted(rows);
                }

                String html = renderUrlPage(id, address, null, checksHtml);
                resp.setContentType("text/html; charset=UTF-8");
                resp.getWriter().write(html);

            } catch (SQLException e) {
                resp.setStatus(500);
                resp.getWriter().write("DB error: " + e.getMessage());
            }

            return;
        }

        resp.setStatus(404);
        resp.getWriter().write("Not found");
    }
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getPathInfo(); // например: /42/checks
        if (path == null) {
            resp.setStatus(404);
            return;
        }

        // ожидаем: /{id}/checks
        String[] parts = path.split("/");
        if (parts.length == 3 && !parts[1].isBlank() && "checks".equals(parts[2])) {
            long id = Long.parseLong(parts[1]);

            String type = req.getParameter("type"); // name="type" из формы
            if (type == null || type.isBlank()) type = "HTTP";

            // 1) вызов MockWebServer
            String remoteResponse;
            try {
                //remoteResponse = callMockCreateCheck(id, type);
                JdbcUtil.insertUrlCheck(ds, id, statusCode);
            } catch (SQLException e) {
                resp.setStatus(502);
                //resp.getWriter().write("Mock call failed: " + e.getMessage());
                resp.getWriter().write("DB insert url_checks failed: " + e.getMessage());
                return;
            }

            // 2) запись проверки в БД
            try {
                JdbcUtil.insertCheck(ds, id, type, remoteResponse);
            } catch (SQLException e) {
                resp.setStatus(500);
                resp.getWriter().write("DB insert failed: " + e.getMessage());
                return;
            }

            // 3) редирект обратно
            resp.sendRedirect("/urls/" + id);
            return;
        }

        resp.setStatus(404);
        resp.getWriter().write("Not found");
    }

    private MockResult callMockCreateCheck(long urlId, String type) throws IOException {
        String url = mockBaseUrl + "mock/checks";

        String json = "{"
                + "\"urlId\":" + urlId + ","
                + "\"type\":\"" + escapeJson(type) + "\""
                + "}";

        RequestBody body = RequestBody.create(
                json,
                MediaType.parse("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        var response = http.newCall(request).execute();
        String respBody = response.body() != null ? response.body().string() : "";

        if (response.code() < 200 || response.code() >= 300) {
            throw new IOException("Mock status=" + response.code() + ", body=" + respBody);
        }

        return new MockResult(respBody, response.code()); // return respBody;
    }
    //private String renderUrlPage(long id, String address, String msg) {
    //    String escapedAddress = escapeHtml(address);
    //    String escapedMsg = msg == null ? "" : escapeHtml(msg);

        // Jetty servlet mapping в контексте "/" => путь для редиректа и form action
        // делаем form action на относительный маршрут /urls/{id}/checks
    //    return """
    //  <!doctype html>
    //  <html lang="ru">
    //  <head><meta charset="utf-8"/><title>URL %d</title></head>
    //  <body>
    //    <h1>URL #%d</h1>
    //    <p>%s</p>

    //    <form method="post" action="/urls/%d/checks">
    //      <label>Тип проверки:
    //        <select name="type">
    //          <option value="HTTP">HTTP</option>
    //          <option value="PING">PING</option>
    //        </select>
    //      </label>
    //      <button type="submit">Создать проверку</button>
    //    </form>

    //    %s
    //  </body>
    //  </html>
    //  """.formatted(id, id, escapedAddress, id, msg == null ? "" : "<p>" + escapedMsg + "</p>");
    //}
    private String renderUrlPage(long id, String address, String msg, String checksHtml) {
        String escapedAddress = escapeHtml(address);
        String escapedMsg = msg == null ? "" : escapeHtml(msg);

        return """
      <!doctype html>
      <html lang="ru">
      <head><meta charset="utf-8"/><title>URL %d</title></head>
      <body>
        <h1>URL #%d</h1>
        <p>%s</p>

        <form method="post" action="/urls/%d/checks">
          <label>Тип проверки:
            <select name="type">
              <option value="HTTP">HTTP</option>
              <option value="PING">PING</option>
            </select>
          </label>
          <button type="submit">Создать проверку</button>
        </form>

        %s

        %s
      </body>
      </html>
      """.formatted(
                id,
                id,
                escapedAddress,
                id,
                checksHtml,
                escapedMsg.isBlank() ? "" : ("<p>" + escapedMsg + "</p>")
        );
    }
    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}