package mdb;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.jms.*;

import rmi.ZoomService;

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
        // DO NOT use resourceAdapter property - TomEE handles this via tomee.xml
    }
)
public class ZoomRequestListener implements MessageListener {

    private static final String[] RMI_HOSTS = {
            "c04-rmi-server-1",
            "c05-rmi-server-2"
    };

    private static final int[] RMI_PORTS = {1104, 1105};

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

            System.out.println("C03: Received message - bytes=" + data.length + 
                             ", zoomType=" + zoomType + ", percent=" + percent);

            // RANDOM RMI SERVER
            int idx = random.nextInt(2);
            String host = RMI_HOSTS[idx];
            int port = RMI_PORTS[idx];

            System.out.println("C03: Calling RMI server " + host + ":" + port);

            Registry registry = LocateRegistry.getRegistry(host, port);
            ZoomService service = (ZoomService) registry.lookup("ZoomService");

            byte[] result = service.zoom(data, zoomType, percent);

            System.out.println("C03: RMI call OK, result bytes = " + result.length);

        } catch (Exception e) {
            System.err.println("C03: ERROR in onMessage:");
            e.printStackTrace();
        }
    }
}