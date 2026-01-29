package mdb;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.jms.*;

import rmi.ZoomService;

import java.io.OutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Random;

@MessageDriven(
    activationConfig = {
        @ActivationConfigProperty(
            propertyName = "destinationType",
            propertyValue = "javax.jms.Topic"
        ),
        @ActivationConfigProperty(
            propertyName = "destination",
            propertyValue = "zoomTopic"
        )
    }
)
public class ZoomRequestListener implements MessageListener {

    private static final String[] RMI_HOSTS = {
            "c04-rmi-server-1",
            "c05-rmi-server-2"
    };

    private static final int[] RMI_PORTS = {1104, 1105};
    
    private static final String NODE_API_URL = "http://c06-node-api:7006/api/pictures";

    private final Random random = new Random();

    @Override
    public void onMessage(Message message) {
        System.out.println("C03: ======= onMessage CALLED =======");
        
        try {
            if (!(message instanceof BytesMessage)) {
                System.out.println("C03: Unsupported message type: " + message.getClass().getName());
                return;
            }

            BytesMessage bm = (BytesMessage) message;

            byte[] data = new byte[(int) bm.getBodyLength()];
            bm.readBytes(data);

            String zoomType = bm.getStringProperty("zoomType");
            int percent = bm.getIntProperty("percent");
            String requestId = bm.getStringProperty("requestId");

            System.out.println("C03: Received message - bytes=" + data.length + 
                             ", zoomType=" + zoomType + ", percent=" + percent +
                             ", requestId=" + requestId);

            // RANDOM RMI SERVER
            int idx = random.nextInt(2);
            String host = RMI_HOSTS[idx];
            int port = RMI_PORTS[idx];

            System.out.println("C03: Calling RMI server " + host + ":" + port);

            Registry registry = LocateRegistry.getRegistry(host, port);
            ZoomService service = (ZoomService) registry.lookup("ZoomService");

            byte[] result = service.zoom(data, zoomType, percent);

            System.out.println("C03: RMI call OK, result bytes = " + result.length);

            // SAVE TO MYSQL via C06 Node API
            // Use requestId as filename so frontend can find it
            String filename = requestId;
            int pictureId = saveToDatabase(result, filename);
            
            System.out.println("C03: Image saved to MySQL with ID = " + pictureId);
            System.out.println("C03: Download URL = http://localhost:7006/api/pictures/name/" + requestId);

        } catch (Exception e) {
            System.err.println("C03: ERROR in onMessage:");
            e.printStackTrace();
        }
    }
    
    private int saveToDatabase(byte[] imageData, String filename) throws Exception {
        String boundary = "----WebKitFormBoundary" + System.currentTimeMillis();
        
        URL url = new URL(NODE_API_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        
        try (OutputStream os = conn.getOutputStream()) {
            // Write multipart form data
            StringBuilder sb = new StringBuilder();
            sb.append("--").append(boundary).append("\r\n");
            sb.append("Content-Disposition: form-data; name=\"file\"; filename=\"").append(filename).append("\"\r\n");
            sb.append("Content-Type: image/bmp\r\n\r\n");
            os.write(sb.toString().getBytes());
            
            // Write image bytes
            os.write(imageData);
            
            // Write closing boundary
            os.write(("\r\n--" + boundary + "--\r\n").getBytes());
        }
        
        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            throw new Exception("Failed to save image, HTTP code: " + responseCode);
        }
        
        // Read response to get picture ID
        try (InputStream is = conn.getInputStream()) {
            byte[] response = is.readAllBytes();
            String json = new String(response);
            // Parse {"id":123} - simple parsing
            int idStart = json.indexOf(":") + 1;
            int idEnd = json.indexOf("}");
            return Integer.parseInt(json.substring(idStart, idEnd).trim());
        }
    }
}