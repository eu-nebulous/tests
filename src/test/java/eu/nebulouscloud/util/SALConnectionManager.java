package eu.nebulouscloud.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.nebulouscloud.model.SALAPIClient;

import eu.nebulouscloud.test.automated.tests.NebulousEndpointConfig;
import org.citrusframework.TestCaseRunner;
import org.citrusframework.http.actions.HttpActionBuilder;
import org.citrusframework.http.client.HttpClient;
import org.citrusframework.message.MessageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.testng.Assert;

import java.util.concurrent.atomic.AtomicBoolean;
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
     */
    public void loginAndGetSessionId(TestCaseRunner runner) {
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
    }

    /**
     * Validates the existence of the specified cloud provider in the SAL API.
     *
     * @param runner The Citrus TestRunner for running Citrus actions.
     * @param uuid   The UUID of the cloud provider to validate.
     */
    public void validateCloudProviders(TestCaseRunner runner, String uuid) {
        // Step 2: Fetch cloud providers
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
                        boolean uuidExists = StreamSupport.stream(jsonArray.spliterator(), false)
                                .anyMatch(node -> uuid.equals(node.get("cloudId").asText()));
                        if (!uuidExists) {
                            throw new RuntimeException("The provided UUID does not exist in the array for any cloudId.");
                        }
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                }));
    }

    /**
     * Placeholder method to get the cluster status.
     *
     * @param runner    The Citrus TestRunner for running Citrus actions.
     * @param clusterName The Name of the cluster to fetch the status for.
     */
    public void getClusterStatus(TestCaseRunner runner, String clusterName) {

        long maxWaitTimeMillis = 40 * 60 * 1000; // 40 minutes in ms
        long retryIntervalMillis = 10 * 1000; // 10 seconds in ms
        long startTime = System.currentTimeMillis();

        AtomicBoolean isDeployed = new AtomicBoolean(false);

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

                        // Parse the payload to extract the "status" field
                        try {
                            JsonNode jsonResponse = objectMapper.readTree(payload);
                            String status = jsonResponse.get("status").asText();
                            logger.info("Cluster status: {}", status);

                            // If the status is "deployed", set the isDeployed flag to true
                            if ("deployed".equalsIgnoreCase(status)) {
                                isDeployed.set(true);
                                logger.info("Cluster successfully reached 'deployed' status.");
                            } else if ("submited".equalsIgnoreCase(status)) {
                                logger.debug("Cluster is still in 'submited' state, retrying...");
                            } else {
                                logger.warn("Unexpected cluster status: {}", status);
                                isDeployed.set(true);
                            }

                        } catch (JsonProcessingException e) {
                            logger.error("Error parsing the cluster status response: {}", e.getMessage(), e);
                            throw new RuntimeException("Error parsing the cluster status response: " + e.getMessage(), e);
                        }
                    }));

            // Wait for the retry interval before trying again
            if (!isDeployed.get()) {
                try {
                    Thread.sleep(retryIntervalMillis);
                } catch (InterruptedException e) {
                    logger.error("Retry sleep interrupted", e);
                    Thread.currentThread().interrupt();
                }
            }
        }

        if (isDeployed.get()) {
            logger.info("Cluster successfully deployed.");
            Assert.assertTrue(isDeployed.get(), "Cluster has been successfully deployed.");
        } else {
            logger.error("Cluster did not reach 'deployed' status.");
            Assert.fail("Cluster did not reach 'deployed' status.");
        }
    }
}
