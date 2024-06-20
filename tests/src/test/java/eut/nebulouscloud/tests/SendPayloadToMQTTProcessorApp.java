package eut.nebulouscloud.tests;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

public class SendPayloadToMQTTProcessorApp {
	static Logger LOGGER = LoggerFactory.getLogger(SendPayloadToMQTTProcessorApp.class);
	static protected ObjectMapper om = new ObjectMapper();
	static int DELAY_SECONDS = 3;
	static MQTTProcessorAppMessageBrokerInterface appBroker;
	static String mqttBroker = "broker.emqx.io";
	static String mqttPort = "1883";
	static String mqttTopicPrefix = "atest";
	static String mqttAppInputTopic = mqttTopicPrefix + "/input";
	static String mqttAppOutputTopic = mqttTopicPrefix + "/output";
	
	public static void main(String[] args) throws Exception
	{
		//String applicationId = "application=1431290905automated-testing-mqtt-app-1715257889393";
		String applicationId = "1549030905automated-testing-mqtt-app-1715262543304";
		LOGGER.info(String.format("Begin MQTT Processor APP deployment. applicationId is %s", applicationId));
		appBroker = new MQTTProcessorAppMessageBrokerInterface("tcp://" + mqttBroker + ":" + mqttPort, mqttAppOutputTopic);
		while(true)
		{
			if(sendPayloadAndWaitAnswer(20) == false)
			{
				break;
			}else
			{
				LOGGER.info("App responded");
			}
			Thread.sleep(1000);
		}
	}
	
	private static boolean sendPayloadAndWaitAnswer(int timeoutSeconds) throws Exception {
		long timeout = new Date().getTime() + (timeoutSeconds * 1000);
		int retriesLeft = 2;
		do {
			/**
			 * Build a request to be sent to the application input topic.
			 */
			Map<String, Object> inferenceRequest = new HashMap<String, Object>();
			inferenceRequest.put("timestamp", new SimpleDateFormat("YYYY-MM-dd HH:mm:ssZ").format(new Date()));
			inferenceRequest.put("job_timestamp", inferenceRequest.get("timestamp"));
			inferenceRequest.put("inference_duration", 5);
			String jobId = UUID.randomUUID().toString();
			inferenceRequest.put("job_id", jobId);
			String payload = om.writeValueAsString(inferenceRequest);
			// Send the request
			appBroker.publish(mqttAppInputTopic, payload);

			/**
			 * Check if the application sends a message to the response channel with
			 * apropriate structure (check it is a JSON and has the job_id value). If found,
			 * we can consider the app is running
			 */
			if (appBroker.findFirst(m -> {
				return m.jsonPayload() != null && jobId.equals(m.jsonPayload().get("job_id"));
			}, 3).isPresent()) {
				return true;
			}

			/**
			 * If there is a message with the content "Error in STOMP connection" it means
			 * that the APP is not able to publish metrics to EMS using STOMP. In this
			 * situation, retry at most two times.
			 */
			if (appBroker.findFirst(m -> "Error in STOMP connection".equals(m.payload), 3).isPresent()) {
				retriesLeft--;
				LOGGER.error("APP is reporting initialization error. Retries left:" + retriesLeft);
				appBroker.clearMessageCache();
				if (retriesLeft == 0)
					return false;
			}


		} while (new Date().getTime() < timeout);
		LOGGER.error("Timeout waiting for a message");
		return false;

	}
	
}
