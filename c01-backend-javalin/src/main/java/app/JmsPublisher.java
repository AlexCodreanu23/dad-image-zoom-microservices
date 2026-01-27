package app;

import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.*;

public class JmsPublisher {

    private static final String BROKER_URL = "tcp://c02-jms-broker:61616";
    private static final String TOPIC_NAME = "bmp.topic";

    public static void publish(byte[] imageBytes, String zoomType, int percent) {

        try {
            ConnectionFactory factory = new ActiveMQConnectionFactory(BROKER_URL);
            Connection connection = factory.createConnection();
            connection.start();

            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Topic topic = session.createTopic(TOPIC_NAME);

            MessageProducer producer = session.createProducer(topic);
            producer.setDeliveryMode(DeliveryMode.PERSISTENT);

            BytesMessage message = session.createBytesMessage();
            message.writeBytes(imageBytes);

            message.setStringProperty("zoomType", zoomType);
            message.setIntProperty("percent", percent);

            producer.send(message);

            System.out.println("JMS message sent to topic:");
            System.out.println(" - bytes: " + imageBytes.length);
            System.out.println(" - zoomType: " + zoomType);
            System.out.println(" - percent: " + percent);

            producer.close();
            session.close();
            connection.close();

        } catch (Exception e) {
            System.err.println("JMS failed (broker not ready yet): " + e.getMessage());
        }
    }
}
