package app;

import io.javalin.Javalin;

public class Main {

    public static void main(String[] args) {
        Javalin app = Javalin.create().start(7001);

        app.get("/health", ctx -> {
            ctx.result("C01 Javalin is UP");
        });
    }
}
