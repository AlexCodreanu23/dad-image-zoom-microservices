package app;

import jakarta.ejb.MessageDriven;
import jakarta.jms.*;

@MessageDriven(mappedName = "topic:imageTopic")
public class ImageMessageListener implements MessageListener {

    @Override
    public void onMessage(Message message) {
        try {
            if (message instanceof BytesMessage) {
                BytesMessage bytesMessage = (BytesMessage) message;

                int size = (int) bytesMessage.getBodyLength();
                byte[] imageBytes = new byte[size];
                bytesMessage.readBytes(imageBytes);

                String zoomType = bytesMessage.getStringProperty("zoomType");
                int percent = bytesMessage.getIntProperty("percent");

                System.out.println("C03 received message");
                System.out.println("bytes = " + size);
                System.out.println("zoomType = " + zoomType);
                System.out.println("percent = " + percent);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
