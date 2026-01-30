package app;

import io.javalin.Javalin;
import io.javalin.http.UploadedFile;
import java.io.InputStream;
import java.util.UUID;

public class Main {
    public static void main(String[] args) {
        Javalin app = Javalin.create(config -> {
            config.plugins.enableCors(cors -> {
                cors.add(it -> it.anyHost());
            });
        }).start(7001);

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

            byte[] imageBytes;
            try (InputStream is = file.content()) {
                imageBytes = is.readAllBytes();
            } catch (Exception e) {
                e.printStackTrace();
                ctx.status(500).result("Error reading BMP file");
                return;
            }

            // Generate unique request ID
            String requestId = UUID.randomUUID().toString();

            try {
                JmsPublisher.publish(imageBytes, zoomType, percent, requestId);
            } catch (Exception e) {
                e.printStackTrace();
                ctx.status(500).result("Error sending JMS message");
                return;
            }

            // Return response with download URL
            // The image will be available at this URL once processing is complete
            String downloadUrl = "http://localhost:7006/api/pictures/name/" + requestId;
            
            ctx.json(new UploadResponse("OK", zoomType, percent, requestId, downloadUrl));
        });
    }

    public static class UploadResponse {
        public String status;
        public String zoomType;
        public int percent;
        public String requestId;
        public String downloadUrl;

        public UploadResponse(String status, String zoomType, int percent, String requestId, String downloadUrl) {
            this.status = status;
            this.zoomType = zoomType;
            this.percent = percent;
            this.requestId = requestId;
            this.downloadUrl = downloadUrl;
        }
    }
}