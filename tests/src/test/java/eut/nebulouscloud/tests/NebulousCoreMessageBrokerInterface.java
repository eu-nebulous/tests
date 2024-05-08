package eut.nebulouscloud.tests;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

import org.apache.qpid.protonj2.client.Message;
import org.apache.qpid.protonj2.client.exceptions.ClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.nebulouscloud.exn.Connector;
import eu.nebulouscloud.exn.core.Consumer;
import eu.nebulouscloud.exn.core.Context;
import eu.nebulouscloud.exn.core.Handler;
import eu.nebulouscloud.exn.core.Publisher;
import eu.nebulouscloud.exn.core.SyncedPublisher;
import eu.nebulouscloud.exn.handlers.ConnectorHandler;
import eu.nebulouscloud.exn.settings.StaticExnConfig;

/***
 * Class for facilitating the interaction with the NebulOuS message broker with connection parameters host:"localhost", port: 5672, user: "admin", password:"admin".
 * Implements several functions to send messages to mimic the behaviour of certain NebulOuS components.
 * Registers any received message and offers methods to query them.
 */
public class NebulousCoreMessageBrokerInterface {
	static Logger LOGGER = LoggerFactory.getLogger(NebulousCoreMessageBrokerInterface.class);
	protected ObjectMapper om = new ObjectMapper();
	Publisher metricModelPublisher;
	Publisher appDeployMessagePublisher;
	SyncedPublisher getClusterPublisher;
	
	/**
	 * Broker connection properties 
	 */
	final String brokerHost = "localhost";
	final int brokerPort = 5672;
	final String brokerUser = "admin";
	final String brokerPassword ="admin";
			
	private List<NebulOuSCoreMessage> messages = Collections.synchronizedList(new LinkedList<NebulOuSCoreMessage>());

	
	public NebulousCoreMessageBrokerInterface() {
		/**
		 * Setup NebulOuS message broker client
		 */
		LOGGER.info("Start NebulOuS message broker client");
		metricModelPublisher = new Publisher("metricModelPublisher", "eu.nebulouscloud.ui.dsl.metric_model", true,
				true);
		appDeployMessagePublisher = new Publisher("appDeployMessagePublisher", "eu.nebulouscloud.ui.dsl.generic", true,
				true);
		getClusterPublisher = new SyncedPublisher("getCluster", "eu.nebulouscloud.exn.sal.cluster.get", true, true);
		Consumer cons1 = new Consumer("monitoring", ">", new MyConsumerHandler(this), true, true);
		Connector myEXNClient = new Connector("thisINotImportant", new MyConnectorHandler(),
				List.of(metricModelPublisher, appDeployMessagePublisher, getClusterPublisher), List.of(cons1), true,
				true,
				new StaticExnConfig(brokerHost, brokerPort, brokerUser, brokerPassword));
		myEXNClient.start();
	}
	
	/**
	 * Waits for a message that matches the given predicate to appear and returns it
	 * (if found). If timeout is reached without the message being recieved, returns
	 * an empty optional.
	 * 
	 * @param appId          the app Id to filter by. If null, no filtering occurs
	 *                       by appId
	 * @param topic          the topic to filter by. If null, no filtering occurs by
	 *                       topic
	 * @param predicate      The search predicate. If null, it is not used.
	 * @param timeoutSeconds The maximum timeout to wait for a message with the
	 *                       given predicate to be found in the list (in seconds).
	 *                       It must be a positive integer or 0.
	 * @return An optional with the first message that matchs the predicate if any
	 *         found.
	 */
	public Optional<NebulOuSCoreMessage> findFirst(String appId, String topic, Predicate<NebulOuSCoreMessage> predicate,
			int timeoutSeconds) {
		Optional<NebulOuSCoreMessage> result = Optional.empty();
		long timeout = new Date().getTime() + (timeoutSeconds * 1000);
		Predicate<NebulOuSCoreMessage> finalPredicate = predicate != null
				? messagesFromAppAndTopic(appId, topic).and(predicate)
				: messagesFromAppAndTopic(appId, topic);
		do {
			synchronized (messages) {

				result = messages.stream().filter(finalPredicate).findFirst();
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

	class MyConsumerHandler extends Handler {
		NebulousCoreMessageBrokerInterface messageStore;

		public MyConsumerHandler(NebulousCoreMessageBrokerInterface messageStore) {
			this.messageStore = messageStore;
		}

		@Override
		public void onMessage(String key, String address, Map body, Message message, Context context) {
			String to = "??";
			try {
				to = message.to() != null ? message.to() : address;
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			Map<Object, Object> props = new HashMap<Object, Object>();

			try {
				message.forEachProperty((k, v) -> props.put(k, v));
			} catch (ClientException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			String subject = "?";
			try {
				subject = message.subject();
			} catch (ClientException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			Object correlationId = 0;
			try {
				correlationId = message.correlationId();
			} catch (ClientException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			LOGGER.trace("\r\n{}\r\nsubject:{}\r\npayload:{}\r\nproperties:{}\r\ncorrelationId:{}", to, subject, body,
					props, correlationId);

			NebulOuSCoreMessage internal = new NebulOuSCoreMessage(new Date(), to, body,
					(String) props.getOrDefault("application", null),
					correlationId != null ? correlationId.toString() : "");
			messageStore.add(internal);
			try {
				LOGGER.trace(om.writeValueAsString(body));
			} catch (JsonProcessingException e) {
			}
		}
	}

	/**
	 * Queries SAL for the status of a cluster
	 * @param clusterName The cluster name.
	 * @return The cluster status, or null in case of error.
	 */
	public String getClusterStatus(String clusterName) {
		Map<String, Object> msg = Map.of("metaData", Map.of("user", "admin", "clusterName", clusterName));
		Map<String, Object> response = getClusterPublisher.sendSync(msg, clusterName, null, false);
		JsonNode payload = extractPayloadFromExnResponse(response, "getCluster");
		if (payload.isMissingNode())
			return null;
		LOGGER.info("isClusterReady: " + payload.toString());
		JsonNode jsonState = payload.at("/status");
		if (jsonState.isMissingNode())
			return null;
		return jsonState.asText();
	}

	/**
	 * Extract and check the SAL response from an exn-middleware response. The SAL
	 * response will be valid JSON encoded as a string in the "body" field of the
	 * response. If the response is of the following form, log an error and return a
	 * missing node instead:
	 *
	 * <pre>{@code
	 * {
	 *   "key": <known exception key>,
	 *   "message": "some error message"
	 * }
	 * }</pre>
	 *
	 * @param responseMessage The response from exn-middleware.
	 * @param caller          Caller information, used for logging only.
	 * @return The SAL response as a parsed JsonNode, or a node where {@code
	 *  isMissingNode()} will return true if SAL reported an error.
	 */
	private JsonNode extractPayloadFromExnResponse(Map<String, Object> responseMessage, String caller) {
		JsonNode response = om.valueToTree(responseMessage);
		String salRawResponse = response.at("/body").asText(); // it's already a string, asText() is for the type system
		JsonNode metadata = response.at("/metaData");
		JsonNode salResponse = om.missingNode(); // the data coming from SAL
		try {
			salResponse = om.readTree(salRawResponse);
		} catch (JsonProcessingException e) {
			LOGGER.error("Could not read message body as JSON: body = '{}', caller = '{}'", salRawResponse, caller, e);
			return om.missingNode();
		}
		if (!metadata.at("/status").asText().startsWith("2")) {
			// we only accept 200, 202, numbers of that nature
			LOGGER.error("exn-middleware-sal request failed with error code '{}' and message '{}', caller '{}'",
					metadata.at("/status"), salResponse.at("/message").asText(), caller);
			return om.missingNode();
		}
		return salResponse;
	}

	private class MyConnectorHandler extends ConnectorHandler {

		public void onReady(AtomicReference<Context> context) {
			LOGGER.info("Optimiser-controller connected to ActiveMQ");
		}
	}


	/**
	 * Build a predicate that filters by messages for a certain appId and topic
	 * @param appId: The appId to filter for. If null, it is ignored
	 * @param topic: The topic to filter for. If null, it is ignored. 
	 * @return
	 */
	public static Predicate<NebulOuSCoreMessage> messagesFromAppAndTopic(String appId, String topic) {
		return messagesFromApp(appId).and((Predicate<NebulOuSCoreMessage>) m -> topic == null || topic.equals(m.topic));
	}

	/**
	 * Builds a predicate that filters messages for the given app ID. If appID is null, the predicate has no effect.
	 * @param id
	 * @return
	 */
	public static Predicate<NebulOuSCoreMessage> messagesFromApp(String id) {
		return ((Predicate<NebulOuSCoreMessage>) m -> id == null || id.equals(m.applicationId));
	}

	/**
	 * Adds a new message to the cache
	 * 
	 * @param message
	 */
	public void add(NebulOuSCoreMessage message) {
		try {
			LOGGER.trace("Adding message:" + om.writeValueAsString(message));
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		this.messages.add(message);
	}
	
	/**
	 * Sends an app creation message as done by the UI during deployment.
	 * @param appCreationPayload
	 * @param applicationId
	 */
	public void sendAppCreationMessage(Map<String, Object> appCreationPayload, String applicationId) {
		appDeployMessagePublisher.send(appCreationPayload, applicationId);

	}
	/**
	 * Sends the metric model message as done by the UI during deployment. 
	 * @param metricModelPayload
	 * @param applicationId
	 */
	public void sendMetricModelMessage(Map<String,Object> metricModelPayload,String applicationId)
	{
		metricModelPublisher.send(metricModelPayload,applicationId);
	}

}
