package hexlet.code;
import io.javalin.Javalin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);
    private final DataSource ds;
    public Main(DataSource ds) {    this.ds = ds;  }
    public static void main(String[] args) {
        // создаём экземпляр и стартуем приложение
        Javalin app = App.getApp();
        // “для разработки”: просто логируйте больше через конфиг Logback (ниже)
        log.info("Starting app...");
        app.start();
    }
    public int countUsers() throws Exception {
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement("select count(*) from users");
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }
}
