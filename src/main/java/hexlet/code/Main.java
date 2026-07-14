package hexlet.code;
import io.javalin.Javalin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);
    public static void main(String[] args) {
        // создаём экземпляр и стартуем приложение
        Javalin app = App.getApp();
        // “для разработки”: просто логируйте больше через конфиг Logback (ниже)
        log.info("Starting app...");
        app.start();
    }
}
