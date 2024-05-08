package eut.nebulouscloud.tests;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

class MQTTProcessorAppDeploymentTest {
	static Logger LOGGER = LoggerFactory.getLogger(MQTTProcessorAppDeploymentTest.class);
	protected ObjectMapper om = new ObjectMapper();
	static int DELAY_SECONDS = 3;

	 String applicationId = new SimpleDateFormat("HHmmssddMM").format(new Date())
	 + "automated-testing-mqtt-app-"
	 + new Date().getTime();
	
	NebulousCoreMessageBrokerInterface coreBroker;
	MQTTProcessorAppMessageBrokerInterface appBroker;
	String mqttBroker = "broker.emqx.io";
	String mqttPort = "1883";
	String mqttTopicPrefix = "atest";
	String mqttAppInputTopic = mqttTopicPrefix + "/input";
	String mqttAppOutputTopic = mqttTopicPrefix + "/output";

	/**
	 * This test ensures that a MQTT processor app can be deployed using NebulOuS. The test
	 * simulates the user requesting a deployment of an app through UI (sending app
	 * creation message and metric model). Then, the test asserts that optimizer
	 * controller performs expected actions, namely: - requesting node candidates
	 * and geting a response from CFSB - defines the app cluster - deploys the app
	 * cluster - sends the AMPL file for the solver - reports app status to be
	 * running
	 * 
	 * Once the optimizer controller reports the app being successfully deployed,
	 * the test asserts that the app works as expected. For this, it connects to a
	 * public MQTT broker where the app is waiting for job requests, publish one and waits for the response
	 * for them.
	 * 
	 * @throws Exception
	 */
	@Test
	void test() throws Exception {

		LOGGER.info(String.format("Begin MQTT Processor APP deployment. applicationId is %s", applicationId));
		coreBroker = new NebulousCoreMessageBrokerInterface();
		appBroker = new MQTTProcessorAppMessageBrokerInterface("tcp://" + mqttBroker + ":" + mqttPort, mqttAppOutputTopic);

		/**
		 * Prepare and send app creation message and assert is correctly received by any subscriber.
		 * 
		 * The app creation message payload template is stored in the project resources folder. This file contains several 
		 * parameters that need to be substituted. These are:
		 *  APP_ID: The id of the app being deployed
		 *  MQTT connection details (APP_MQTT_BROKER_SERVER, APP_MQTT_BROKER_PORT, APP_MQTT_INPUT_TOPIC, APP_MQTT_OUTPUT_TOPIC): On startup, the application connects to the configured MQTT broker 
		 *  and waits for messages on the topic APP_MQTT_INPUT_TOPIC. Uppon a  well structured message on said topic, the application simulates some work and sends a message to  APP_MQTT_OUTPUT_TOPIC.
		 *  REPORT_METRICS_TO_EMS: If true, application tries to connect to local EMS broker to report metrics. If false, not.
		 * 
		 */
		LOGGER.info("send app creation message");
		
		Map<String, String> appParameters = new HashMap<String, String>();
		appParameters.put("{{APP_ID}}", applicationId);
		appParameters.put("{{APP_MQTT_BROKER_SERVER}}", mqttBroker);
		appParameters.put("{{APP_MQTT_BROKER_PORT}}", mqttPort);
		appParameters.put("{{APP_MQTT_INPUT_TOPIC}}", "$share/workers/" + mqttAppInputTopic);
		appParameters.put("{{APP_MQTT_OUTPUT_TOPIC}}", mqttAppOutputTopic);
		appParameters.put("{{REPORT_METRICS_TO_EMS}}", "True");
		
		Map<String, Object> appCreationPayload = FileTemplatingUtils
				.loadJSONFileAndSubstitute("mqtt_processor_app/app_creation_message.json", appParameters);
		coreBroker.sendAppCreationMessage(appCreationPayload, applicationId);

		// Assert that the message was sent
		assertTrue(coreBroker.findFirst(applicationId, "eu.nebulouscloud.ui.dsl.generic", null, 10).isPresent());
		Thread.sleep(DELAY_SECONDS * 1000);

		/**
		 * Send metric model and assert is correctly received by any subscriber
		 */
		LOGGER.info("send metric model");
		Map<String, Object> metricModelPayload = FileTemplatingUtils.loadJSONFileAndSubstitute("mqtt_processor_app/metric_model.json",
				Map.of("{{APP_ID}}", applicationId));
		coreBroker.sendMetricModelMessage(metricModelPayload, applicationId);
		assertTrue(coreBroker.findFirst(applicationId, "eu.nebulouscloud.ui.dsl.metric_model", null, 10).isPresent());

		/**
		 * Assert that Optimizer controller requests for node candidates for the
		 * application cluster
		 */
		LOGGER.info("Wait for optimizer to request node candidates");

		Optional<NebulOuSCoreMessage> nodeRequestToCFSB = coreBroker.findFirst(applicationId,
				"eu.nebulouscloud.cfsb.get_node_candidates", null, 10);
		assertTrue(nodeRequestToCFSB.isPresent());
		assertNotNull(nodeRequestToCFSB.get().correlationId);

		Optional<NebulOuSCoreMessage> nodeRequestToSAL = coreBroker.findFirst(applicationId,
				"eu.nebulouscloud.exn.sal.nodecandidate.get", null, 10);
		assertTrue(nodeRequestToSAL.isPresent());
		assertNotNull(nodeRequestToSAL.get().correlationId);
		/**
		 * Assert that SAL anwsers the request
		 */
		LOGGER.info("Wait for CFSB to recieve an answer on node candidates from SAL");
		assertTrue(coreBroker.findFirst(applicationId, "eu.nebulouscloud.exn.sal.nodecandidate.get.reply",
				m -> nodeRequestToSAL.get().correlationId.equals(m.correlationId), 30).isPresent());
		
		/**
		 * Assert that CFSB anwsers the request
		 */
		LOGGER.info("Wait for optimizer to recieve an answer on node candidates from CFSB");
		assertTrue(coreBroker.findFirst(applicationId, "eu.nebulouscloud.cfsb.get_node_candidates.reply",
				m -> nodeRequestToCFSB.get().correlationId.equals(m.correlationId), 30).isPresent());

		/**
		 * Assert that optimiser defines the cluster
		 */
		LOGGER.info("Wait for optimizer to define cluster");
		Optional<NebulOuSCoreMessage> defineClusterRequest = coreBroker.findFirst(applicationId,
				"eu.nebulouscloud.exn.sal.cluster.define", null, 80);
		assertTrue(defineClusterRequest.isPresent());
		LOGGER.info(om.writeValueAsString(defineClusterRequest.get().payload));
		// Retrieve the name of the new cluster
		String clusterName = (String) om
				.readValue((String) defineClusterRequest.get().payload.get("body"), HashMap.class).get("name");

		LOGGER.info(String.format("Cluster name: %s", clusterName));

		/**
		 * Assert that Optimiser deploys the cluster
		 */
		LOGGER.info("Wait for optimizer to deploy cluster");
		assertTrue(
				coreBroker.findFirst(applicationId, "eu.nebulouscloud.exn.sal.cluster.deploy", null, 80).isPresent());

		LOGGER.info("Wait for a message from optimizer controller to solver with the AMPL File");
		assertTrue(
				coreBroker.findFirst(applicationId, "eu.nebulouscloud.exn.sal.cluster.deploy", null, 80).isPresent());

		LOGGER.info("Wait for cluster to be ready");
		waitForCluster(clusterName, 60 * 10);

		LOGGER.info("Wait for APP state to be Running");
		assertTrue(waitForAppRunning(60 * 10));

		LOGGER.info("Wait for APP to be operative");
		assertTrue(checkApplicationWorks(60 * 10));
		// myEXNClient.stop();

	}

	/**
	 * Checks that the application is working by sending an input message through
	 * the app message broker and expecting the apropriate answer from the
	 * application throught the same app message broker. If the application reports
	 * a problem with STOMP communication for publishing metrics to EMS "Error in
	 * STOMP connection", retry 2 times and give up.
	 * 
	 * @param timeoutSeconds The ammount of seconds to wait for an answer
	 * @return true if the application responded, false otherwise
	 * @throws Exception
	 */
	private boolean checkApplicationWorks(int timeoutSeconds) throws Exception {
		long timeout = new Date().getTime() + (timeoutSeconds * 1000);
		int retriesLeft = 2;
		do {
			/**
			 * Build a request to be sent to the application input topic.
			 */
			Map<String, Object> inferenceRequest = new HashMap<String, Object>();
			inferenceRequest.put("timestamp", new SimpleDateFormat("YYYY-MM-dd HH:mm:ssZ").format(new Date()));
			inferenceRequest.put("job_timestamp", inferenceRequest.get("timestamp"));
			inferenceRequest.put("inference_duration", 1);
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

	private boolean waitForCluster(String clusterName, int timeoutSeconds) {
		long timeout = new Date().getTime() + (timeoutSeconds * 1000);
		do {
			String status = coreBroker.getClusterStatus(clusterName);
			if (status == null || "submited".equals(status)) {
				try {
					Thread.sleep(10000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				continue;
			}
			if ("deployed".equals(status)) {
				return true;
			}
			return false;
		} while (new Date().getTime() < timeout);
		LOGGER.error("Timeout waiting for a message");
		return false;
	}

	/**
	 * Wait for the optimizer controller to report that the application is on status "RUNNING" (return true) or "FAILED" (return false).  
	 * 
	 * <ul>
	 * <li>NEW: The application has been created from the GUI and is waiting for the
	 * performance indicators from the utility evaluator. *
	 * <li>READY: The application is ready for deployment.
	 * <li>DEPLOYING: The application is being deployed or redeployed.
	 * <li>RUNNING: The application is running.
	 * <li>FAILED: The application is in an invalid state: one or more messages
	 * could not be parsed, or deployment or redeployment failed.
	 * 
	 * @param timeoutSeconds
	 * @return True if the optimizer controller reported the app to be running, false if the optimizer controller the app to have failed or the timeout is reached.
	 */
	private boolean waitForAppRunning(int timeoutSeconds) {

		long timeout = new Date().getTime() + (timeoutSeconds * 1000);
		do {
			/**
			 * Check if app status is reported to be running
			 */
			if (coreBroker.findFirst(applicationId, "eu.nebulouscloud.optimiser.controller.app_state",
					m -> "RUNNING".equals(m.payload.get("state")), 2).isPresent()) {
				return true;
			}
			/**
			 * Check if APP status is failed.
			 */
			if (coreBroker.findFirst(applicationId, "eu.nebulouscloud.optimiser.controller.app_state",
					m -> "FAILED".equals(m.payload.get("state")), 2).isPresent()) {
				return false;
			}
			try {
				Thread.sleep(10000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

		} while (new Date().getTime() < timeout);
		LOGGER.error("Timeout waiting for a message");
		return false;

	}

}