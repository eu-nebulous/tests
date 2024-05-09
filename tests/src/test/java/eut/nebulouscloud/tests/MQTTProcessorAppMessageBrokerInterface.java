package eut.nebulouscloud.tests;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/***
 * Class for facilitating the interaction with the MQTT broker used by the MQTT processing APP.
 * Registers any received message and offers methods to query them.
 * Implements a method for facilitating sending messages to the processing app using MQTT.
 */
public class MQTTProcessorAppMessageBrokerInterface {

	static Logger LOGGER = LoggerFactory.getLogger(MQTTProcessorAppMessageBrokerInterface.class);
	private final AtomicBoolean messageRecieved = new AtomicBoolean(false);	
	MqttClient client;
	protected ObjectMapper om = new ObjectMapper();
	private List<SimpleMQTTMessage> messages = Collections.synchronizedList(new LinkedList<SimpleMQTTMessage>());
	
	public class SimpleMQTTMessage {
		final protected ObjectMapper om = new ObjectMapper();
		final String topic;
		final String payload;
		final Date date;

		public SimpleMQTTMessage(String topic, String payload) {
			super();
			this.topic = topic;
			this.payload = payload;
			this.date = new Date();
		}

		public Map<String, Object> jsonPayload() {
			try {
				return om.readValue(payload, HashMap.class);
			} catch (JsonProcessingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return null;
		}

	}
	
	public void publish(String topic,String payload)
	{
		MqttMessage m = new MqttMessage();
		m.setQos(2);
		m.setPayload(payload.getBytes());
		try {
			client.publish(topic, m);
			LOGGER.info("Message published: "+payload);
		} catch (MqttException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public MQTTProcessorAppMessageBrokerInterface(String broker, String baseTopic) {
		try {
			LOGGER.info("Connecting to broker: " + broker);
			client = new MqttClient(broker, MqttClient.generateClientId(), new MemoryPersistence());
			MqttConnectOptions connOpts = new MqttConnectOptions();
			connOpts.setCleanSession(true);			
			
			client.connect(connOpts);
			LOGGER.info("Connected");

			client.setCallback(new MqttCallback() {
				@Override
				public void connectionLost(Throwable throwable) {
					LOGGER.error("Connection lost!");
				}

				@Override
				public void messageArrived(String topic, MqttMessage message) throws Exception {
					String payload = new String(message.getPayload());
					LOGGER.info("Message received: " + payload);
					messages.add(new SimpleMQTTMessage(topic, payload));
				}

				@Override
				public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
				}
			});
			client.subscribe(baseTopic+"/#");
		} catch (MqttException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Waits for a message that matches the given predicate to appear and returns it
	 * (if found). If timeout is reached without the message being recieved, returns
	 * an empty optional.
	 * 
	 * @param predicate      The search predicate. If null, it is not used.
	 * @param timeoutSeconds The maximum timeout to wait for a message with the
	 *                       given predicate to be found in the list (in seconds).
	 *                       It must be a positive integer or 0.
	 * @return An optional with the first message that matchs the predicate if any
	 *         found.
	 */
	public Optional<SimpleMQTTMessage> findFirst(Predicate<SimpleMQTTMessage> predicate, int timeoutSeconds) {
		Optional<SimpleMQTTMessage> result = Optional.empty();
		long timeout = new Date().getTime() + (timeoutSeconds * 1000);
		do {
			synchronized (messages) {
				result = messages.stream().filter(predicate).findFirst();
			}
			if (result.isEmpty() && new Date().getTime() < timeout) {
				LOGGER.error(String.format("Waiting for message. %.2fs left for timeout.",
						((timeout - new Date().getTime()) / 1000.0)));
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		} while (result.isEmpty() && new Date().getTime() < timeout);
		if (new Date().getTime() > timeout) {
			LOGGER.error("Timeout waiting for a message");
		}
		return result;

	};
	
	/**
	 * Remove all messages from the cache
	 */
	public void clearMessageCache()
	{
		synchronized (messages) {
			messages.clear();
		}
	}

	
}