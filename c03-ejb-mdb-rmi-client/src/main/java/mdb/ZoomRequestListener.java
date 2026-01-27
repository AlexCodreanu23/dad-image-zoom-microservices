package mdb;

import jakarta.ejb.ActivationConfigProperty;
import jakarta.ejb.MessageDriven;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;
import jakarta.jms.TextMessage;
import jakarta.jms.BytesMessage;


@MessageDriven(
    activationConfig = {
        @ActivationConfigProperty(
            propertyName = "destinationType",
            propertyValue = "jakarta.jms.Topic"
        ),
        @ActivationConfigProperty(
            propertyName = "destination",
            propertyValue = "zoom-topic"
        )
    }
)
public class ZoomRequestListener implements MessageListener {

    @Override
        public void onMessage(Message message) {
            try {
                BytesMessage bm = (BytesMessage) message;
                System.out.println("C03 received message");
                System.out.println("Bytes length: " + bm.getBodyLength());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
}
