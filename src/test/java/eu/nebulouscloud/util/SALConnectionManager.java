package eu.nebulouscloud.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.nebulouscloud.model.CloudResources;
import eu.nebulouscloud.model.SALAPIClient;

import org.citrusframework.TestCaseRunner;
import org.citrusframework.http.actions.HttpActionBuilder;
import org.citrusframework.http.client.HttpClient;
import org.citrusframework.message.MessageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;
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

        long maxWaitTimeMillis = 60 * 60 * 1000; // 60 minutes in ms
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
                            } else if ("submitted".equalsIgnoreCase(currentStatus) || /* currentStatus == null || */ "defined".equalsIgnoreCase(currentStatus)) {
                                logger.info("Cluster is still in {},retrying.....",currentStatus);
                                logger.debug("Cluster is still in 'submitted', 'defined' state or status is null, retrying...");
                            } else {
                                logger.warn("Unexpected cluster status: {}. Stop Checking", currentStatus);
                                isDeployed.set(false);
                            }

                        } catch (JsonProcessingException e) {
                            logger.error("Error parsing the cluster status response: {}", e.getMessage(), e);
                            throw new RuntimeException("Error parsing the cluster status response: " + e.getMessage(), e);
                        }
                    }));

            // Handle unexpected status
            if (!isDeployed.get()) {
                String currentStatus = status.get();
                if (currentStatus == null) {

                    logger.warn("Unexpected cluster status: '{}'. Exiting polling loop.", currentStatus);
                    break;
                }

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
    /**
     * Registers a new cloud in SAL using the /cloud endpoint.
     *
     * @param runner                    The Citrus TestRunner.
     * @param cloudRegistrationPayload A Map representing the JSON body for the POST request.
     * @return true if the cloud was registered successfully, false otherwise.
     */
    public boolean addCloud(TestCaseRunner runner, List<Map<String, Object>> cloudRegistrationPayload) {
        try {
            runner.run(HttpActionBuilder.http()
                    .client(salEndpoint)
                    .send()
                    .post("/cloud")
                    .message()
                    .type(MessageType.JSON)
                    .header("Content-Type", "application/json")
                    .header("sessionid", salapiClient.getSessionId())
                    .body(objectMapper.writeValueAsString(cloudRegistrationPayload)));

            runner.run(HttpActionBuilder.http()
                    .client(salEndpoint)
                    .receive()
                    .response(HttpStatus.OK));

            logger.info("Cloud registration completed successfully.");
            return true;
        } catch (Exception e) {
            logger.error("Cloud registration failed: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Retrieves cloud resources from the SAL API based on the provided cloud name.
     *
     * @param runner    The Citrus TestRunner for running Citrus actions.
     * @param cloudName The name of the cloud to retrieve resources for.
     * @return A {@link CloudResources} object containing details of the cloud if found, or null if not found.
     */
    public CloudResources getCloud(TestCaseRunner runner, String cloudName) {
        AtomicReference<CloudResources> cloudResource = new AtomicReference<>();

        runner.run(HttpActionBuilder.http()
                .client(salEndpoint)
                .send()
                .get("/cloud")
                .message()
                .type(MessageType.JSON)
                .header("sessionid", salapiClient.getSessionId()));

        runner.run(HttpActionBuilder.http()
                .client(salEndpoint)
                .receive()
                .response(HttpStatus.OK)
                .message()
                .validate((message, context) -> {
                    String payload = message.getPayload().toString();
                    try {
                        JsonNode cloudList = objectMapper.readTree(payload);
                        for (JsonNode cloud : cloudList) {
                            if (cloud.has("cloudId") && cloudName.equals(cloud.get("cloudId").asText())) {
                                JsonNode deployedRegions = cloud.get("deployedRegions");
                                String regions = (deployedRegions != null && deployedRegions.fieldNames().hasNext())
                                        ? deployedRegions.fieldNames().next()
                                        : "";

                                cloudResource.set(new CloudResources(
                                        cloud.get("cloudId").asText(),
                                        cloud.get("cloudId").asText(),
                                        cloud.get("cloudProvider").asText(),
                                        "true",
                                        regions
                                ));
                                break;
                            }
                        }
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to parse cloud resources: " + e.getMessage(), e);
                    }
                }));
        logger.info("Cloud resource: {}", cloudResource.toString());
        return cloudResource.get();
    }

    /**
     * Checks if there is any asynchronous operation currently running on the SAL API.
     *
     * @param runner The Citrus TestRunner used to perform HTTP requests and actions.
     * @return true if any asynchronous operation is running, false otherwise.
     */
    public boolean isAnyAsyncNode(TestCaseRunner runner) {
        long maxWaitTimeMillis = 60 * 60 * 1000;
        long retryIntervalMillis = 40 * 1000;
        long startTime = System.currentTimeMillis();

        AtomicBoolean isAsyncRunning = new AtomicBoolean(true);

        while (isAsyncRunning.get() && (System.currentTimeMillis() - startTime) < maxWaitTimeMillis) {
            runner.run(HttpActionBuilder.http()
                    .client(salEndpoint)
                    .send()
                    .get("/cloud/async")
                    .message()
                    .header("sessionid", salapiClient.getSessionId()));

            runner.run(HttpActionBuilder.http()
                    .client(salEndpoint)
                    .receive()
                    .response(HttpStatus.OK)
                    .message()
                    .validate((message, context) -> {
                        String payload = message.getPayload().toString();
                        logger.info("Async node response: {}", payload);
                        isAsyncRunning.set(Boolean.parseBoolean(payload));
                    }));

            if (isAsyncRunning.get()) {
                try {
                    Thread.sleep(retryIntervalMillis);
                } catch (InterruptedException e) {
                    logger.error("Retry sleep interrupted", e);
                    Thread.currentThread().interrupt();
                }
            }
        }

        return isAsyncRunning.get();
    }
}