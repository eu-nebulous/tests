package eu.nebulouscloud.test.automated.tests;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.nebulouscloud.exceptions.InvalidFormatException;
import eu.nebulouscloud.exceptions.MissingConfigValueException;
import eu.nebulouscloud.model.CloudResources;
import eu.nebulouscloud.model.NebulousCoreMessage;
import eu.nebulouscloud.util.FileTemplatingUtils;
import eu.nebulouscloud.util.MessageSender;
import eu.nebulouscloud.util.SALConnectionManager;
import eu.nebulouscloud.util.StringToMapParser;
import org.citrusframework.TestCaseRunner;
import org.citrusframework.TestCaseRunnerFactory;
import org.citrusframework.annotations.CitrusTest;
import org.citrusframework.context.TestContext;
import org.citrusframework.context.TestContextFactory;
import org.citrusframework.exceptions.ActionTimeoutException;
import org.citrusframework.exceptions.CitrusRuntimeException;
import org.citrusframework.http.client.HttpClient;
import org.citrusframework.jms.endpoint.JmsEndpoint;
import org.citrusframework.testng.spring.TestNGCitrusSpringSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ContextConfiguration;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.citrusframework.actions.ReceiveMessageAction.Builder.receive;
import static org.testng.Assert.assertTrue;

/**
 * TC_24
 * App deployment on NebulOuS-managed node fails due to lack of resources
 *
 */
@ContextConfiguration(classes = {NebulousEndpointConfig.class})
public class AppDeploymentNodeFailureTest extends TestNGCitrusSpringSupport {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private TestContext context;

    private TestCaseRunner runner;

    private final StringToMapParser parser = new StringToMapParser();

    private MessageSender messageSender;
    private SALConnectionManager salConnectionManager;

    String applicationId = new SimpleDateFormat("HHmmssddMM").format(new Date())
            + "-automated-testing-app-"
            + new Date().getTime();


    @Autowired
    @Qualifier("appCreationEndpoint")
    private JmsEndpoint appCreationEndpoint;

    @Autowired
    @Qualifier("metricModelEndpoint")
    private JmsEndpoint metricModelEndpoint;

    @Autowired
    @Qualifier("evaluatorEndpoint")
    private JmsEndpoint evaluatorEndpoint;

    @Autowired
    @Qualifier("nodeCandidatesRequestCFSBEndpoint")
    private JmsEndpoint nodeCandidatesRequestCFSBEndpoint;

    @Autowired
    @Qualifier("nodeCandidatesRequestSALEndpoint")
    private JmsEndpoint nodeCandidatesRequestSALEndpoint;

    @Autowired
    @Qualifier("nodeCandidatesReplySALEndpoint")
    private JmsEndpoint nodeCandidatesReplySALEndpoint;

    @Autowired
    @Qualifier("nodeCandidatesReplyCFSBEndpoint")
    private JmsEndpoint nodeCandidatesReplyCFSBEndpoint;

    @Autowired
    @Qualifier("defineClusterEndpoint")
    private JmsEndpoint defineClusterEndpoint;

    @Autowired
    @Qualifier("deployClusterEndpoint")
    private JmsEndpoint deployClusterEndpoint;

    @Autowired
    @Qualifier("getAMPLfileEndpoint")
    private JmsEndpoint getAMPLfileEndpoint;

    @Autowired
    @Qualifier("appStatusEndpoint")
    private JmsEndpoint appStatusEndpoint;

    @Autowired
    @Qualifier("salEndpoint")
    private HttpClient salEndpoint;

    @Autowired
    private Environment env;

    @BeforeMethod
    public void createTestContext() {
        context = TestContextFactory.newInstance().getObject();
        runner = TestCaseRunnerFactory.createRunner(context);

        String qpidAddress = env.getProperty("qpid-jms.address");
        int qpidPort = Integer.parseInt(env.getProperty("qpid-jms.port"));
        String qpidUsername = env.getProperty("qpid-jms.username");
        String qpidPassword = env.getProperty("qpid-jms.password");

        // Initialize MessageSender and SALConnectionManager
        messageSender = new MessageSender(qpidAddress, qpidPort, qpidUsername, qpidPassword, applicationId);
        salConnectionManager = new SALConnectionManager(salEndpoint, objectMapper);
    }

    @Test
    @CitrusTest
    public void test() throws Exception {
        Map<String, String> selectorMap = new HashMap<>();
        selectorMap.put("application", "<applicationId>");
        String clusterName = "<clusterName>";


        /*
         * Assert that Optimiser deploys the cluster
         **/
        logger.info("Wait for optimizer to deploy cluster");
        $(receive(deployClusterEndpoint)
                .message()
                .selector(selectorMap)
                .timeout(8000)
                .validate((message, context) -> {
                    // print debug message
                    logger.debug("Message that optimizer deploys the cluster received");
                    logger.info(message.getPayload().toString());
                    // Ignore body
                })
        );

        /*
         * Assert that the cluster is ready
         **/
        Assert.assertEquals(salConnectionManager.getClusterStatus(runner, clusterName), "deployed", "Cluster has been successfully deployed");

        /*
         * Assert that App is ready and running
         **/
        NebulousCoreMessage appStatus = new NebulousCoreMessage();
        AtomicBoolean success = new AtomicBoolean(false);
        int retryIntervalMillis = 5000;
        AtomicBoolean keepLooping = new AtomicBoolean(true);
        while (keepLooping.get() && !success.get()) {
            try {
                $(receive(appStatusEndpoint)
                        .message()
                        .name("appStatus")
                        .selector(selectorMap)
                        .timeout(60 * 60 * 1000) //60min 60sec 1000ms
                        .validate((message, context) -> {
                            // Print debug message
                            logger.debug("Message of app status");
                            logger.debug(message.getPayload().toString());
                            try {
                                Map<String, Object> messageMap = parser.parseStringToMap(message.getPayload().toString());
                                appStatus.setPayload(messageMap);

                                String state = (String) appStatus.getPayload().get("state");

                                if ("RUNNING".equals(state)) {
                                    success.set(true);
                                    keepLooping.set(false);
                                } else if ("FAILED".equals(state)) {
                                    keepLooping.set(false);
                                    throw new CitrusRuntimeException("Received message with state FAILED, stopping the test.");
                                } else {
                                    logger.info("Received message with state: " + state + ". Continuing to check...");
                                }
                            } catch (InvalidFormatException e) {
                                logger.error("Failed to parse input: " + e.getMessage());
                            }
                        })
                );
            } catch (ActionTimeoutException e) {
                logger.warn("No message received within timeout, retrying...");
            } catch (AssertionError | CitrusRuntimeException e) {
                logger.error("Validation failed or message state is 'FAILED', stopping the test.", e);
                throw e;  // Propagate the error if state is "FAILED" or validation failed
            }

            // Wait for the retry interval if not yet successful
            if (!success.get() && keepLooping.get()) {
                try {
                    Thread.sleep(retryIntervalMillis);
                } catch (InterruptedException ie) {
                    logger.error("Sleep interrupted", ie);
                }
            }
        }
        if (success.get()) {
            logger.info("App successfully reached the 'RUNNING' state.");
            Assert.assertTrue(success.get(), "App has been successfully deployed and is running.");
        } else {
            logger.error("App did not reach the 'RUNNING' state within the timeout period.");
            Assert.fail("App did not reach the 'RUNNING' state within the timeout period.");
        }
    }
}