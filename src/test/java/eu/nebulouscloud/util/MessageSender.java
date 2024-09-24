package eu.nebulouscloud.util;

import eu.nebulouscloud.model.NebulousCoreMessage;
import eu.nebulouscloud.test.automated.tests.NebulousEndpointConfig;
import org.apache.qpid.protonj2.client.*;

import org.apache.qpid.protonj2.client.exceptions.ClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Utility class for sending messages via Qpid Protonj2 Client.
 */
public class MessageSender {
    static Logger logger = LoggerFactory.getLogger(MessageSender.class);
    private final String address;
    private final int port;
    private final String username;
    private final String password;
    private final String applicationId;

    /**
     * Constructor to initialize connection details for sending messages.
     *
     * @param address       The address of the Qpid broker.
     * @param port          The port of the Qpid broker.
     * @param username      The username for authentication.
     * @param password      The password for authentication.
     * @param applicationId The ID of the application sending messages.
     */
    public MessageSender(String address, int port, String username, String password, String applicationId) {
        this.address = address;
        this.port = port;
        this.username = username;
        this.password = password;
        this.applicationId = applicationId;
    }

    /**
     * Sends a NebulousCoreMessage to the designated topic.
     *
     * @param message The NebulousCoreMessage object to be sent.
     */
    public void sendMessage(NebulousCoreMessage message) {
        Client client = Client.create();

        ConnectionOptions options = new ConnectionOptions();
        options.user(username);
        options.password(password);

        try (Connection connection = client.connect(address, port, options);
             Sender sender = connection.openSender(message.getTopic())) {

            Message<Map<String, Object>> protonMessage = Message.create(message.getPayload());
            protonMessage.subject(applicationId);
            protonMessage.property("application", applicationId);

            sender.send(protonMessage);
            logger.debug("Message sent successfully to topic: " + message.getTopic());

        } catch (ClientException e) {
            throw new RuntimeException("Failed to send message", e);
        }
    }
}