package hexlet.code;
import io.javalin.Javalin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
public class Main {
    private static final Logger logVar = LoggerFactory.getLogger(Main.class);
    private final DataSource ds;
    public Main(DataSource ds) {
        this.ds = ds;
    }
    public static void main(String[] args) {
        // создаём экземпляр и стартуем приложение
        try {
            Javalin app = App.getApp();
            // “для разработки”: просто логируйте больше через конфиг Logback (ниже)
            logVar.info("Starting app...");
            app.start();
        } catch (Exception e) {
            System.err.println("Не удалось запустить приложение");
            e.printStackTrace();
            System.exit(1);
        }

    }
    /**
     * Возвращает количество пользователей в базе данных.
     *
     * @return число записей в таблице {@code users}
     * @throws Exception если не удалось получить соединение с базой данных
     *                   или выполнить запрос
     */
    public int countUsers() throws Exception {
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement("select count(*) from users");
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }
}
