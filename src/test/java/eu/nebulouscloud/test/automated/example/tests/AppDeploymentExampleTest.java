package eu.nebulouscloud.test.automated.example.tests;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.nebulouscloud.exceptions.InvalidFormatException;
import eu.nebulouscloud.model.CloudResources;
import eu.nebulouscloud.model.NebulousCoreMessage;
import eu.nebulouscloud.test.automated.tests.NebulousEndpointConfig;
import eu.nebulouscloud.util.FileTemplatingUtils;
import eu.nebulouscloud.exceptions.MissingConfigValueException;
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
import org.citrusframework.exceptions.ValidationException;
import org.citrusframework.http.client.HttpClient;
import org.citrusframework.jms.endpoint.JmsEndpoint;
import org.citrusframework.message.Message;
import org.citrusframework.message.MessageType;
import org.citrusframework.testng.spring.TestNGCitrusSpringSupport;
import org.citrusframework.validation.MessageValidator;
import org.citrusframework.validation.context.ValidationContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.StreamSupport;

import static org.citrusframework.actions.ReceiveMessageAction.Builder.receive;
import static org.citrusframework.http.actions.HttpActionBuilder.http;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

@ContextConfiguration(classes = {NebulousEndpointConfig.class})
public class AppDeploymentExampleTest extends TestNGCitrusSpringSupport {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private TestContext context;

    private TestCaseRunner runner;

    private final StringToMapParser parser = new StringToMapParser();

    private MessageSender messageSender;
    private SALConnectionManager salConnectionManager;

    String applicationId = new SimpleDateFormat("HHmmssddMM").format(new Date())
            + "automated-testing-mqtt-app-"
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


    String mqttBroker = "broker.emqx.io";
    String mqttPort = "1883";
    String mqttTopicPrefix = applicationId.replaceAll("-", "");
    String mqttAppInputTopic = mqttTopicPrefix + "/input";
    String mqttAppOutputTopic = mqttTopicPrefix + "/output";

    @BeforeMethod
    public void createTestContext() {
        context = TestContextFactory.newInstance().getObject();
        runner  = TestCaseRunnerFactory.createRunner(context);

        String qpidAddress = env.getProperty("qpid-jms.address");
        int qpidPort = Integer.parseInt(env.getProperty("qpid-jms.port"));
        String qpidUsername = env.getProperty("qpid-jms.username");
        String qpidPassword = env.getProperty("qpid-jms.password");

        // Initialize MessageSender and SALConnectionManager
        messageSender = new MessageSender(qpidAddress, qpidPort, qpidUsername, qpidPassword, applicationId);
        salConnectionManager = new SALConnectionManager(salEndpoint, objectMapper);

//        context.getMessageValidatorRegistry().addMessageValidator("simple", new MessageValidator<>() {
//            @Override
//            public void validateMessage(Message receivedMessage, Message controlMessage, TestContext context, List<ValidationContext> validationContexts) throws ValidationException {
//                Assert.assertEquals(receivedMessage.getPayload(), controlMessage.getPayload());
//            }
//
//            @Override
//            public boolean supportsMessageType(String messageType, Message message) {
//                return true;
//            }
//        });
    }

    @Test
    @CitrusTest
    public void test() throws Exception {
        Map<String, String> appParameters = new HashMap<>();
        appParameters.put("{{APP_ID}}", applicationId);
        appParameters.put("{{APP_MQTT_BROKER_SERVER}}", mqttBroker);
        appParameters.put("{{APP_MQTT_BROKER_PORT}}", mqttPort);
        appParameters.put("{{APP_MQTT_INPUT_TOPIC}}", "$share/workers/" + mqttAppInputTopic);
        appParameters.put("{{APP_MQTT_OUTPUT_TOPIC}}", mqttAppOutputTopic);
        appParameters.put("{{REPORT_METRICS_TO_EMS}}", "True");
        appParameters.put("{{APP_CPU}}", "4.0");
        appParameters.put("{{APP_RAM}}", "8048Mi");
        appParameters.put("{{APP_EMS_PORT}}", "61610");
        appParameters.put("{{APP_EMS_USER}}", env.getProperty("app.ems.username"));
        appParameters.put("{{APP_EMS_PASSWORD}}", env.getProperty("app.ems.password"));

        Map<String, Object> appCreationPayload = FileTemplatingUtils
                .loadJSONFileAndSubstitute("mqtt_processor_app/app_creation_message.json", appParameters);
        ArrayList<Object> envVars = ((ArrayList<Object>) appCreationPayload.get("environmentVariables"));

        appCreationPayload.put("content",
                FileTemplatingUtils.loadFileAndSubstitute("mqtt_processor_app/kubevela.yaml", appParameters));

        ArrayList<Object> resources = ((ArrayList<Object>) appCreationPayload.get("resources"));
        resources.clear();
//        resources.add(Map.of("uuid", "aws-automated-testing", "title", "", "platform", "", "enabled", "true", "regions", "us-east-1"));
//        resources.add(Map.of("uuid", "c9a625c7-f705-4128-948f-6b5765509029", "title", "blah", "platform", "AWS", "enabled", "true","regions","us-east-1"));
//        resources.add(Map.of("uuid", "uio-openstack-optimizer", "title", "whatever", "platform", "whatever", "enabled", "true","regions","bgo"));

        /**
         * Config the cloud id
         */
        CloudResources cloudResource = new CloudResources(
                Optional.ofNullable(env.getProperty("cloud_resources.uuid")).orElseThrow(() -> new MissingConfigValueException("cloud_resources.uuid")),
                Optional.ofNullable(env.getProperty("cloud_resources.title")).orElseThrow(() -> new MissingConfigValueException("cloud_resources.title")),
                Optional.ofNullable(env.getProperty("cloud_resources.platform")).orElseThrow(() -> new MissingConfigValueException("cloud_resources.platform")),
                Optional.ofNullable(env.getProperty("cloud_resources.enabled")).orElseThrow(() -> new MissingConfigValueException("cloud_resources.enabled")),
                Optional.ofNullable(env.getProperty("cloud_resources.regions")).orElseThrow(() -> new MissingConfigValueException("cloud_resources.regions"))
        );
        resources.add(cloudResource.toMap());


        // Configure docker registry
        envVars.add(Map.of("name", "PRIVATE_DOCKER_REGISTRY_SERVER", "value", Optional.ofNullable(env.getProperty("docker.server")).orElseThrow(() ->new MissingConfigValueException("docker.server")),"secret","false"));
        envVars.add(Map.of("name", "PRIVATE_DOCKER_REGISTRY_USERNAME", "value", Optional.ofNullable(env.getProperty("docker.username")).orElseThrow(() ->new MissingConfigValueException("docker.username")),"secret","false"));
        envVars.add(Map.of("name", "PRIVATE_DOCKER_REGISTRY_PASSWORD", "value", Optional.ofNullable(env.getProperty("docker.password")).orElseThrow(() ->new MissingConfigValueException("docker.password")),"secret","false"));
        envVars.add(Map.of("name", "PRIVATE_DOCKER_REGISTRY_EMAIL", "value", Optional.ofNullable(env.getProperty("docker.email")).orElseThrow(() ->new MissingConfigValueException("docker.email")),"secret","false"));
        envVars.add(Map.of("name", "ONM_URL", "value", Optional.ofNullable(env.getProperty("onm_url")).orElseThrow(() ->new MissingConfigValueException("onm_url")),"secret","false"));



        // Test SAL connection and cloud providers
        salConnectionManager.loginAndGetSessionId(runner);
//        salConnectionManager.connectAndValidateCloudProvider(runner, cloudResource.getUuid());

        /**
         * Header Selectors for receiving published message
         */
        Map<String, String> selectorMap = new HashMap<>();
        selectorMap.put("application", applicationId);

        logger.info(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(appCreationPayload));

        NebulousCoreMessage appCreationMessage = new NebulousCoreMessage(appCreationPayload,env.getProperty("jms.topic.nebulous.optimiser"));
        messageSender.sendMessage(appCreationMessage);



        $(receive(appCreationEndpoint)
                .message()
                .selector(selectorMap)
                .validate((message, context) -> {
                    // print debug message
                    logger.debug("appCreationPayload payload received");
                    // Ignore body
                }));

        /**
         * Send metric model and assert is correctly received by any subscriber
         */
        Map<String, Object> metricModelPayload = FileTemplatingUtils.loadJSONFileAndSubstitute("mqtt_processor_app/metric_model.json",
                Map.of("{{APP_ID}}", applicationId));


        logger.info(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(metricModelPayload));

        NebulousCoreMessage metricModelMessage = new NebulousCoreMessage(metricModelPayload,env.getProperty("jms.topic.nebulous.metric_model"));
        messageSender.sendMessage(metricModelMessage);


        $(receive(metricModelEndpoint)
                .message()
                .selector(selectorMap)
                .validate((message, context) -> {
                    // print debug message
                    logger.debug("metricModelPayload payload received");
                    // Ignore body
                })
        );
        /**
         * Wait for utility evaluator to start
         */
        logger.info("Wait for utility evaluator to start");
        $(receive(evaluatorEndpoint)
                .message()
                .selector(selectorMap)
                .timeout(10000)
                .validate((message, context) -> {
                    // print debug message
                    logger.debug("Message from Evaluator received");
                    // Ignore body
                })
        );


        /**
         * Assert that Optimizer controller requests for node candidates for the
         * application cluster
         */
        NebulousCoreMessage nodeRequestToCFSBmessage = new NebulousCoreMessage();
        logger.info("Wait for optimizer to request node candidates");
        $(receive(nodeCandidatesRequestCFSBEndpoint)
                .message()
                .selector(selectorMap)
                .timeout(100000)
                .validate((message, context) -> {
                    // print debug message
                    logger.debug("Message to request candidates received");
                    assertTrue(message.getHeader("citrus_jms_correlationId") != null);
                    nodeRequestToCFSBmessage.setCorrelationId(message.getHeader("citrus_jms_correlationId").toString());
                    // Ignore body
                })
        );

        NebulousCoreMessage nodeRequestToSALmessage = new NebulousCoreMessage();
        $(receive(nodeCandidatesRequestSALEndpoint)
                .message()
                .selector(selectorMap)
                .timeout(10000)
                .validate((message, context) -> {
                    // print debug message
                    logger.debug("Message to request candidates received from SAL");
                    assertTrue(message.getHeader("citrus_jms_correlationId") != null);
                    nodeRequestToSALmessage.setCorrelationId(message.getHeader("citrus_jms_correlationId").toString());
                    // Ignore body
                })
        );

        /**
         * Assert that SAL anwsers the request
         */
        logger.info("Wait for CFSB to recieve an answer on node candidates from SAL");
        $(receive(nodeCandidatesReplySALEndpoint)
                .message()
                .selector(selectorMap)
                .timeout(3000)
                .validate((message, context) -> {
                    // print debug message
                    logger.debug("Message that CFSB receives an answer on node candidates from SAL , received");
                    assertTrue(nodeRequestToSALmessage.getCorrelationId().equals(message.getHeader("citrus_jms_correlationId").toString()));
                    // Ignore body
                })
        );


        /**
         * Assert that CFSB anwsers the request
         */
        logger.info("Wait for optimizer to recieve an answer on node candidates from CFSB");
        $(receive(nodeCandidatesReplyCFSBEndpoint)
                .message()
                .selector(selectorMap)
                .timeout(8000)
                .validate((message, context) -> {
                    // print debug message
                    logger.debug("Message that optimizer receives an answer on node candidates from CFSB , received");
                    assertTrue(nodeRequestToCFSBmessage.getCorrelationId().equals(message.getHeader("citrus_jms_correlationId").toString()));
                    // Ignore body
                })
        );

        /**
         * Wait for optimizer to define cluster
         */
        NebulousCoreMessage defineCluster = new NebulousCoreMessage();
        logger.info("Wait for optimizer to define cluster");
        $(receive(defineClusterEndpoint)
                .message()
                .selector(selectorMap)
                .timeout(8000)
                .validate((message, context) -> {
                    // print debug message
                    logger.debug("Message that optimizer defined the cluster received");
                    try {
                        Map<String, Object> messageMap = parser.parseStringToMap(message.getPayload().toString());
                        defineCluster.setPayload(messageMap);
                    } catch (InvalidFormatException e) {
                        logger.error("Failed to parse input: " + e.getMessage());
                    }
                })
        );
        String clusterName = null;
        if (defineCluster.getPayload().containsKey("body")) {
            Object bodyObject = defineCluster.getPayload().get("body");

            if (bodyObject instanceof Map) {
                Map<String, Object> bodyMap = (Map<String, Object>) bodyObject;
                Object nameObject = bodyMap.get("name");

                if (nameObject instanceof String name) {
                    logger.info("Cluster name: " + name);
                    clusterName = name;
                } else {
                    logger.error("Name is not a string.");
                }
            } else {
                logger.error("Body is not a map.");
            }
        } else {
            logger.error("Result does not contain 'body'.");
        }

        /**
         * Assert that Optimiser deploys the cluster
         */
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
        //TODO To be changed with relevant endpoint according to the ordiginal !?
        logger.info("Wait for a message from optimizer controller to solver with the AMPL File");
        $(receive(getAMPLfileEndpoint)
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

        /**
         * Assert that the cluster is ready
         */
        Assert.assertEquals(salConnectionManager.getClusterStatus(runner, clusterName), "deployed", "Cluster has been successfully deployed");

        /**
         * Assert that App is ready and running
         */
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
                            logger.info(message.getPayload().toString());
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


    @Test
    @CitrusTest
    public void singleEndpointTest()  {

        JmsEndpoint endpoint = appStatusEndpoint;
        Map<String, String> selectorMap = new HashMap<>();
        selectorMap.put("application", "2118133008automated-testing-mqtt-app-1725041893407");

        $(receive(endpoint)
                .message()
                .selector(selectorMap)
                .timeout(4000)
                .validate((message, context) -> {
                    // Print debug message
                    logger.debug("Testing endpoint");
                    // Ignore body
                    System.out.println(message);
                })
        );
    }

    @Test
    @CitrusTest
    public void salEndpointTest()  {
        $(http()
                .client(salEndpoint)
                .send()
                .get("/cloud")
                .message()
                .type(MessageType.JSON)
                .header("sessionid","dd9147e191845c154b51356e789a8a0a0118c8dd9147e191317403b48000")
        );

        $(http()
                .client(salEndpoint)
                .receive()
                .response(HttpStatus.OK)
                .message()
                .validate((message, context) -> {
                    String payload = message.getPayload().toString();
                    try {
                        JsonNode jsonArray = objectMapper.readTree(payload);
                        assertTrue(jsonArray.isArray() && jsonArray.isEmpty(), "JSON array should be empty");
                        System.out.println(message.getPayload().toString());
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                })
        );
    }
}
