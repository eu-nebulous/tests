package eu.nebulouscloud.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.nebulouscloud.model.SALAPIClient;
import jakarta.servlet.http.Cookie;
import org.citrusframework.TestCaseRunner;
import org.citrusframework.http.actions.HttpActionBuilder;
import org.citrusframework.http.client.HttpClient;
import org.citrusframework.message.MessageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.testng.Assert;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.StreamSupport;

/**
 * Handling Resource Manager connection and adding new resources.
 */

@Component
public class ResourceManager {

    static Logger logger = LoggerFactory.getLogger(ResourceManager.class);
    private final HttpClient rmEndpoint;
    private final ObjectMapper objectMapper;
    private final Environment env;

    public ResourceManager(HttpClient rmEndpoint, ObjectMapper objectMapper, Environment env) {
        this.rmEndpoint = rmEndpoint;
        this.objectMapper = objectMapper;
        this.env = env;
    }

    /**
     * Logs into the Resource Manager and stores the session ID in the Citrus context.
     *
     * @param runner The Citrus TestRunner for running Citrus actions.
     */
    public boolean loginAndGetSessionId(TestCaseRunner runner) {
        // Step 1: Login request
        runner.run(HttpActionBuilder.http()
                .client(rmEndpoint)
                .send()
                .post("/login")
                .message()
                .contentType("application/x-www-form-urlencoded")
                .body("username=" + env.getProperty("resource_manager.username") +
                        "&password=" + env.getProperty("resource_manager.password")));
//                        "&password=aaa"));

        // Step 2: Receive response, check "Location" header, and extract JSESSIONID
        final boolean[] loginSuccess = {false};
        runner.run(HttpActionBuilder.http()
                .client(rmEndpoint)
                .receive()
                .response()
                .message()
                .validate((message, testContext) -> {
                    //  Check "Location" header
                    String locationHeader = message.getHeader("Location").toString();
                    if (locationHeader != null && locationHeader.endsWith("error")) {
                        logger.warn("Login Failed! Redirected to error page: {}", locationHeader);
                        Assert.assertTrue(loginSuccess[0]);
                    }
                    logger.debug("Login Successful!");

                    //  Extract JSESSIONID if present
                    String rawCookie = message.getHeader("citrus_http_cookie_JSESSIONID").toString();
                    if (rawCookie != null) {
                        String sessionId = rawCookie.replaceAll(".*JSESSIONID=([^;]+);.*", "$1");
                        testContext.setVariable("JSESSIONID", sessionId);
                        logger.debug("Extracted JSESSIONID: {}", sessionId);
                        loginSuccess[0] = true;
                    } else {
                        logger.warn("JSESSIONID cookie not found.");
                    }
                    Assert.assertTrue(loginSuccess[0]);
                }));

        return loginSuccess[0]; // ✅ Return true only if login was successful
    }

    /**
     * Registers a device using the session ID stored in the Citrus context.
     *
     * @param runner The Citrus TestRunner for running Citrus actions.
     * @param jsonPayload The JSON payload for device registration.
     * @return true if registration is successful, false otherwise.
     */
    public boolean registerDevice(TestCaseRunner runner, String jsonPayload) {
        // Retrieve JSESSIONID from Citrus context
        runner.run(context -> {
            String sessionId = context.getVariable("JSESSIONID", String.class);
            if (sessionId == null || sessionId.isEmpty()) {
                logger.warn("Cookie is missing. Please login first.");
                throw new RuntimeException("Session ID not found in context!");
            }
        });

        // Send the PUT request with JSESSIONID from context
        runner.run(HttpActionBuilder.http()
                .client(rmEndpoint)
                .send()
                .put("/discovery/request")
                .message()
                .contentType("application/json")
                .header("Cookie", "JSESSIONID=${JSESSIONID}") // Retrieve from context
                .body(jsonPayload));

        // Receive and validate the response
        final boolean[] registrationSuccess = {false}; // Store result inside lambda
        runner.run(HttpActionBuilder.http()
                .client(rmEndpoint)
                .receive()
                .response(HttpStatus.OK)
                .message()
                .validate((message, testContext) -> {
                    logger.debug("Device Registration Response: {}", message);

                    // Parse response JSON
                    ObjectMapper objectMapper = new ObjectMapper();
                    String responsePayload = message.getPayload(String.class);
                    JsonNode jsonResponse = null;
                    try {
                        jsonResponse = objectMapper.readTree(responsePayload);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                    // Extract "status" field from the JSON object
                    JsonNode statusNode = jsonResponse.get("status");
                    if (statusNode != null && "NEW_REQUEST".equals(statusNode.asText())) {
                        registrationSuccess[0] = true;
                        logger.debug("Device Registration Successful with status: NEW_REQUEST");
                    } else {
                        logger.warn("Device Registration Response: {}", message);
                        logger.warn("Device Registration Failed. Response status: {}", statusNode);
                    }

                    Assert.assertEquals("NEW_REQUEST", statusNode.asText());
                }));

        return registrationSuccess[0]; // Return true only if status is "NEW_REQUEST"
    }
}
