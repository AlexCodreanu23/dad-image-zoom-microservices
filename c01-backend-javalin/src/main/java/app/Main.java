package app;

import io.javalin.Javalin;
import io.javalin.http.UploadedFile;

import java.io.InputStream;

public class Main {

    public static void main(String[] args) {

        Javalin app = Javalin.create().start(7001);

        app.get("/health", ctx -> {
            ctx.result("C01 Javalin is UP");
        });

        app.post("/upload", ctx -> {

            String zoomType = ctx.formParam("zoomType");
            String percentStr = ctx.formParam("percent");

            if (zoomType == null || percentStr == null) {
                ctx.status(400).result("Missing zoomType or percent");
                return;
            }

            if (!zoomType.equals("in") && !zoomType.equals("out")) {
                ctx.status(400).result("zoomType must be 'in' or 'out'");
                return;
            }

            int percent;
            try {
                percent = Integer.parseInt(percentStr);
            } catch (NumberFormatException e) {
                ctx.status(400).result("percent must be a number");
                return;
            }

            if (percent <= 0 || percent > 500) {
                ctx.status(400).result("percent must be between 1 and 500");
                return;
            }

            UploadedFile file = ctx.uploadedFile("file");
            if (file == null) {
                ctx.status(400).result("Missing BMP file");
                return;
            }

            if (!file.filename().toLowerCase().endsWith(".bmp")) {
                ctx.status(400).result("Only BMP files are allowed");
                return;
            }

            try (InputStream is = file.content()) {
                byte[] imageBytes = is.readAllBytes();

                System.out.println("Received BMP:");
                System.out.println(" - filename: " + file.filename());
                System.out.println(" - size: " + imageBytes.length + " bytes");
                System.out.println(" - zoomType: " + zoomType);
                System.out.println(" - percent: " + percent);

            } catch (Exception e) {
                ctx.status(500).result("Error reading file");
                return;
            }

            ctx.json(new UploadResponse("OK", zoomType, percent));
        });
    }

    public static class UploadResponse {
        public String status;
        public String zoomType;
        public int percent;

        public UploadResponse(String status, String zoomType, int percent) {
            this.status = status;
            this.zoomType = zoomType;
            this.percent = percent;
        }
    }
}
