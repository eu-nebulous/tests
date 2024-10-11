package eu.nebulouscloud.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.nebulouscloud.model.SALAPIClient;

import org.citrusframework.TestCaseRunner;
import org.citrusframework.http.actions.HttpActionBuilder;
import org.citrusframework.http.client.HttpClient;
import org.citrusframework.message.MessageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.StreamSupport;

/**
 * Manager for handling SAL connections, validating cloud providers, and managing clusters.
 */
public class SALConnectionManager {

    static Logger logger = LoggerFactory.getLogger(SALConnectionManager.class);
    private final HttpClient salEndpoint;
    private final ObjectMapper objectMapper;
    private final SALAPIClient salapiClient;

    /**
     * Constructor to initialize the SALConnectionManager.
     *
     * @param salEndpoint  The HttpClient for SAL communication.
     * @param objectMapper Jackson's ObjectMapper for JSON parsing.
     */
    public SALConnectionManager(HttpClient salEndpoint, ObjectMapper objectMapper) {
        this.salEndpoint = salEndpoint;
        this.objectMapper = objectMapper;
        this.salapiClient = new SALAPIClient(); // Initialize the final field
    }

    /**
     * Logs into the SAL API and retrieves the session ID.
     *
     * @param runner The Citrus TestRunner for running Citrus actions.
     * @return
     */
    public boolean loginAndGetSessionId(TestCaseRunner runner) {
        // Step 1: Connect to SAL to get session ID
        runner.run(HttpActionBuilder.http()
                .client(salEndpoint)
                .send()
                .post("/pagateway/connect")
                .message());

        runner.run(HttpActionBuilder.http()
                .client(salEndpoint)
                .receive()
                .response(HttpStatus.OK)
                .message()
                .validate((message, context) -> {
                    String sessionId = message.getPayload().toString();
                    salapiClient.setSessionId(sessionId); // Store the session ID in the salapiClient
                    logger.debug("Session ID: " + sessionId);
                }));
        return salapiClient.getSessionId() != null;
    }

    /**
     * Validates the existence of the specified cloud provider in the SAL API.
     *
     * @param runner The Citrus TestRunner for running Citrus actions.
     * @param uuid   The UUID of the cloud provider to validate.
     */
    public boolean validateCloudProviders(TestCaseRunner runner, String uuid) {
        // Step 2: Fetch cloud providers

        AtomicBoolean uuidExists = new AtomicBoolean(false);
        runner.run(HttpActionBuilder.http()
                .client(salEndpoint)
                .send()
                .get("/cloud")
                .message()
                .type(MessageType.JSON)
                .header("sessionid", salapiClient.getSessionId()));

        // Step 3: Validate the cloud provider exists
        runner.run(HttpActionBuilder.http()
                .client(salEndpoint)
                .receive()
                .response(HttpStatus.OK)
                .message()
                .validate((message, context) -> {
                    String payload = message.getPayload().toString();
                    try {
                        JsonNode jsonArray = objectMapper.readTree(payload);
                        if (!jsonArray.isArray() || jsonArray.isEmpty()) {
                            throw new RuntimeException("JSON array shouldn't be empty");
                        }
                         uuidExists.set(StreamSupport.stream(jsonArray.spliterator(), false)
                                 .anyMatch(node -> uuid.equals(node.get("cloudId").asText())));
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                }));
        return uuidExists.get();
    }

    /**
     * Method to get the cluster status.
     *
     * @param runner    The Citrus TestRunner for running Citrus actions.
     * @param clusterName The Name of the cluster to fetch the status for.
     * @return The final status of the cluster.
     */
    public String getClusterStatus(TestCaseRunner runner, String clusterName) {

        long maxWaitTimeMillis = 40 * 60 * 1000; // 40 minutes in ms
        long retryIntervalMillis = 20 * 1000; // 10 seconds in ms
        long startTime = System.currentTimeMillis();

        AtomicBoolean isDeployed = new AtomicBoolean(false);
        AtomicReference<String> status = new AtomicReference<>(null);  // Use AtomicReference to store the final status

        while (!isDeployed.get() && (System.currentTimeMillis() - startTime) < maxWaitTimeMillis) {
            // Step 1: Send a request to fetch the cluster status
            runner.run(HttpActionBuilder.http()
                    .client(salEndpoint)
                    .send()
                    .get("/cluster/" + clusterName)
                    .message()
                    .header("sessionid", salapiClient.getSessionId()));

            // Step 2: Receive the cluster status
            runner.run(HttpActionBuilder.http()
                    .client(salEndpoint)
                    .receive()
                    .response(HttpStatus.OK)
                    .message()
                    .validate((message, context) -> {
                        String payload = message.getPayload().toString();
                        logger.info(payload);

                        // Parse the payload to extract the "status" field
                        try {
                            JsonNode jsonResponse = objectMapper.readTree(payload);
                            String currentStatus = jsonResponse.get("status") != null ? jsonResponse.get("status").asText() : null;
                            status.set(currentStatus);  // Set the status using AtomicReference
                            logger.info("Cluster status: {}", currentStatus);

                            // If the status is "deployed", set the isDeployed flag to true and exit the loop
                            if ("deployed".equalsIgnoreCase(currentStatus)) {
                                isDeployed.set(true);
                                logger.info("Cluster successfully reached 'deployed' status.");
                            } else if ("submited".equalsIgnoreCase(currentStatus) || currentStatus == null || "defined".equalsIgnoreCase(currentStatus)) {
                                logger.debug("Cluster is still in 'submited', 'defined' state or status is null, retrying...");
                            } else {
                                logger.warn("Unexpected cluster status: {}, stopping the check.", currentStatus);
                                isDeployed.set(false);
                            }

                        } catch (JsonProcessingException e) {
                            logger.error("Error parsing the cluster status response: {}", e.getMessage(), e);
                            throw new RuntimeException("Error parsing the cluster status response: " + e.getMessage(), e);
                        }
                    }));

            // Wait for the retry interval before trying again
            if (!isDeployed.get() && "deployed".equalsIgnoreCase(status.get())) {
                break;  // Break the loop if it is deployed
            }

            if (!isDeployed.get()) {
                try {
                    Thread.sleep(retryIntervalMillis);
                } catch (InterruptedException e) {
                    logger.error("Retry sleep interrupted", e);
                    Thread.currentThread().interrupt();
                }
            }
        }

        return status.get();
    }

}
