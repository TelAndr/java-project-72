package hexlet.code;
import io.javalin.Javalin;

//import static java.lang.reflect.Array.get;

import io.javalin.http.Context;
import static io.javalin.config.JavalinConfig.config;
public class App {
    private static Javalin appInstance;

    public static Javalin getApp() {
        if (appInstance != null) return appInstance;

        appInstance = Javalin.create(config -> {
            config.app4567Port(7000); // при желании замените/уберите
            config.enableCorsForAllOrigins();
            config.bundledPlugins.enableDevLogging();
        }).routes(() -> {
            get("/", ctx -> ctx.result("Hello World"));

            // пример: healthcheck
            get("/health", ctx -> ctx.json(new HealthResponse(true)));
        });
        //appInstance = Javalin.create(config -> {
        //    config.bundledPlugins.enableDevLogging();
        //});
        //appInstance.get("/", ctx -> ctx.result("Hello World"));
        //appInstance.start(7070);
        return appInstance;
    }

    private record HealthResponse(boolean ok) {}

    // пример запуска:
    public static void main(String[] args) {
        App.getApp().start();
    }
}
