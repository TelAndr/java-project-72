package hexlet.code;
import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.resolve.ResourceCodeResolver;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.rendering.template.JavalinJte;
import static io.javalin.apibuilder.ApiBuilder.*;
public class App {
    private static TemplateEngine createTemplateEngine() {
        ClassLoader classLoader = App.class.getClassLoader();
        ResourceCodeResolver codeResolver = new ResourceCodeResolver("templates", classLoader);
        TemplateEngine templateEngine = TemplateEngine.create(codeResolver, ContentType.Html);
        return templateEngine;
    }
    private static Javalin appInstance;

    public static Javalin getApp() {
        if (appInstance != null) return appInstance;

        appInstance = Javalin.create(config -> {
            //config.app4567Port(7000); // при желании замените/уберите
            config.jetty.port = 7000;
            config.enableCorsForAllOrigins();
            config.bundledPlugins.enableDevLogging();
            config.fileRenderer(new JavalinJte(createTemplateEngine()));
            config.routes.apiBuilder(() -> {
                get("/", ctx -> ctx.result("Hello World"));
                get("/health", ctx -> ctx.json(new HealthResponse(true)));
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
        return appInstance;
    }

    private record HealthResponse(boolean ok) {}

    // пример запуска:
    public static void main(String[] args) {
        App.getApp().start();
    }
}
