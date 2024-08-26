package ubi.tests;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import exceptions.MissingConfigValueException;
import model.SALAPIClient;
import org.apache.qpid.protonj2.client.*;
import org.apache.qpid.protonj2.client.exceptions.ClientException;
import org.citrusframework.annotations.CitrusTest;
import org.citrusframework.http.client.HttpClient;
import org.citrusframework.jms.endpoint.JmsEndpoint;
import org.citrusframework.message.MessageType;
import org.citrusframework.testng.spring.TestNGCitrusSpringSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;
import ubi.util.FileTemplatingUtils;

import java.text.SimpleDateFormat;
import java.util.*;

import static org.citrusframework.actions.ReceiveMessageAction.Builder.receive;
import static org.citrusframework.http.actions.HttpActionBuilder.http;
import static org.testng.Assert.assertTrue;

@ContextConfiguration(classes = {NebulousEndpointConfig.class})
public class AppDeploymentTest extends TestNGCitrusSpringSupport {

    private final ObjectMapper objectMapper = new ObjectMapper();
    static int DELAY_SECONDS = 3;

    String applicationId = new SimpleDateFormat("HHmmssddMM").format(new Date())
            + "automated-testing-mqtt-app-"
            + new Date().getTime();

    String mqttBroker = "broker.emqx.io";
    String mqttPort = "1883";
    String mqttTopicPrefix = applicationId.replaceAll("-", "");
    String mqttAppInputTopic = mqttTopicPrefix + "/input";
    String mqttAppOutputTopic = mqttTopicPrefix + "/output";

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
    @Qualifier("salEndpoint")
    private HttpClient salEndpoint;

    @Autowired
    private Environment env;

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

        // Configure cloud id
        ArrayList<Object> resources = ((ArrayList<Object>) appCreationPayload.get("resources"));
        resources.clear();
//        resources.add(Map.of("uuid", "aws-automated-testing", "title", "", "platform", "", "enabled", "true", "regions", "us-east-1"));
//        resources.add(Map.of("uuid", "c9a625c7-f705-4128-948f-6b5765509029", "title", "blah", "platform", "AWS", "enabled", "true","regions","us-east-1"));
//        resources.add(Map.of("uuid", "uio-openstack-optimizer", "title", "whatever", "platform", "whatever", "enabled", "true","regions","bgo"));
        resources.add(Map.of(
                "uuid", Optional.ofNullable(env.getProperty("cloud_resources.uuid")).orElseThrow(() ->new MissingConfigValueException("cloud_resources.uuid")),
                "title", Optional.ofNullable(env.getProperty("cloud_resources.title")).orElseThrow(() ->new MissingConfigValueException("cloud_resources.title")),
                "platform", Optional.ofNullable(env.getProperty("cloud_resources.platform")).orElseThrow(() ->new MissingConfigValueException("cloud_resources.platform")),
                "enabled", Optional.ofNullable(env.getProperty("cloud_resources.enabled")).orElseThrow(() ->new MissingConfigValueException("cloud_resources.enabled")),
                "regions",Optional.ofNullable(env.getProperty("cloud_resources.regions")).orElseThrow(() ->new MissingConfigValueException("cloud_resources.regions"))
        ));

        // Configure docker registry
        envVars.add(Map.of("name", "PRIVATE_DOCKER_REGISTRY_SERVER", "value", Optional.ofNullable(env.getProperty("docker.server")).orElseThrow(() ->new MissingConfigValueException("docker.server")),"secret","false"));
        envVars.add(Map.of("name", "PRIVATE_DOCKER_REGISTRY_USERNAME", "value", Optional.ofNullable(env.getProperty("docker.username")).orElseThrow(() ->new MissingConfigValueException("docker.username")),"secret","false"));
        envVars.add(Map.of("name", "PRIVATE_DOCKER_REGISTRY_PASSWORD", "value", Optional.ofNullable(env.getProperty("docker.password")).orElseThrow(() ->new MissingConfigValueException("docker.password")),"secret","false"));
        envVars.add(Map.of("name", "PRIVATE_DOCKER_REGISTRY_EMAIL", "value", Optional.ofNullable(env.getProperty("docker.email")).orElseThrow(() ->new MissingConfigValueException("docker.email")),"secret","false"));
        envVars.add(Map.of("name", "ONM_URL", "value", Optional.ofNullable(env.getProperty("onm_url")).orElseThrow(() ->new MissingConfigValueException("onm_url")),"secret","false"));



        salConnectionAndCloudProvidersTest();

        /**
         * Header Selectors for receiving published message
         */
        Map<String, String> selectorMap = new HashMap<>();
        selectorMap.put("application", applicationId);

        logger.info(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(appCreationPayload));

        sendMessage(appCreationPayload,env.getProperty("jms.topic.nebulous.optimiser"));

        //citrus Implementation
        //        $(send(appCreationEndpoint)
//                .message()
//                .header("application", applicationId)
//                        .type(applicationId)
////                .header("subject", applicationId)
////                        .header("Content-Type", "application/json")
////                .body(new MessagePayloadBuilder() {
////                    @Override
////                    public Object buildPayload(TestContext testContext) {
////                        try {
////                            return objectMapper.writeValueAsString(appCreationPayload);
////                        } catch (JsonProcessingException e) {
////                            throw new RuntimeException(e);
////                        }
////                    }
////                })
//                .body(appCreationPayload.toString())

//                .body(appCreationPayload.toString())
//        );

        $(receive(appCreationEndpoint)
                .message()
                .selector(selectorMap)
                .validate((message, context) -> {
                    // print debug message
                    System.out.println(message);
                    logger.debug("appCreationPayload payload received");
                    // Ignore body
                }));


        /**
         * Send metric model and assert is correctly received by any subscriber
         */
        Map<String, Object> metricModelPayload = FileTemplatingUtils.loadJSONFileAndSubstitute("mqtt_processor_app/metric_model.json",
                Map.of("{{APP_ID}}", applicationId));


        logger.info(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(metricModelPayload));

        sendMessage(metricModelPayload,env.getProperty("jms.topic.nebulous.metric_model"));

        //Citrus Implementation
//        $(send(metricModelEndpoint)
//                .message()
//                .header("application", applicationId)
//                .header("subject", applicationId)
////                        .body(new MessagePayloadBuilder() {
////                            @Override
////                            public Object buildPayload(TestContext testContext) {
////                                try {
////                                    return objectMapper.writeValueAsString(metricModelPayload);
////                                } catch (JsonProcessingException e) {
////                                    throw new RuntimeException(e);
////                                }
////                            }
////                        })
//                .body(marshal(metricModelPayload,objectMapper))
////                .body(metricModelPayload.toString())
//        );

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
        logger.info("Wait for optimizer to request node candidates");
        $(receive(nodeCandidatesRequestCFSBEndpoint)
                .message()
                .selector(selectorMap)
                .timeout(100000)
                .validate((message, context) -> {
                    // print debug message
                    logger.debug("Message to request candidates received");
                    // Ignore body
                })
        );


        $(receive(nodeCandidatesRequestSALEndpoint)
                .message()
                .selector(selectorMap)
                .timeout(10000)
                .validate((message, context) -> {
                    // print debug message
                    logger.debug("Message to request candidates received from SAL");
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
                    // Ignore body
                })
        );

        /**
         * Wait for optimizer to define cluster
         */
        logger.info("Wait for optimizer to define cluster");
        $(receive(defineClusterEndpoint)
                .message()
                .selector(selectorMap)
                .timeout(8000)
                .validate((message, context) -> {
                    // print debug message
                    logger.debug("Message that optimizer defined the cluster received");
                    logger.info(message.getPayload().toString());
                    // Ignore body
                })
        );

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
                    // Ignore body
                })
        );
    }

    public void sendMessage(Map<String, Object> content, String topic) throws Exception {

        String address = env.getProperty("qpid-jms.address");
        Integer port = Integer.valueOf(env.getProperty("qpid-jms.port"));
        String username = env.getProperty("qpid-jms.username");
        String password = env.getProperty("qpid-jms.password");
        topic = "topic://"+topic;


        Client client = Client.create();

        // Establish a connection to the broker
        ConnectionOptions connectionOptions = new ConnectionOptions();
        connectionOptions.user(username);
        connectionOptions.password(password);

        try (Connection connection = client.connect(address, port, connectionOptions)) {
            // Create a sender to the specified topic
            try (Sender sender = connection.openSender(topic)) {

                Message<Map<String, Object>> message = Message.create(content);

                // Set as message properties the applicationId
                message.subject(applicationId);
                message.property("application", applicationId);

                sender.send(message);

                logger.debug("Message sent successfully to topic: " + topic);
            }
        } catch (ClientException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to send message", e);
        }
    }

    @Test
    @CitrusTest
    public void singleEndpointTest()  {

        JmsEndpoint endpoint = defineClusterEndpoint;
        Map<String, String> selectorMap = new HashMap<>();
        selectorMap.put("application", "0207312608automated-testing-mqtt-app-1724627251238");

        $(receive(endpoint)
                .message()
                .selector(selectorMap)
                .timeout(4000)
                .validate((message, context) -> {
                    // Print debug message
                    logger.debug("Testing endpoint");
                    // Ignore body
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

    public void salConnectionAndCloudProvidersTest(){
        /**
         * Assert of SAL connection
         */
        $(http()
                .client(salEndpoint)
                .send()
                .post("/pagateway/connect")
                .message()
        );
        SALAPIClient salapiClient = new SALAPIClient();
        $(http()
                .client(salEndpoint)
                .receive()
                .response(HttpStatus.OK)
                .message()
                .validate((message, context) -> {
                    // Print debug message
                    logger.info("Sessionid: "+message.getPayload().toString());
                    salapiClient.setSessionId(message.getPayload().toString());
                })
        );

        $(http()
                .client(salEndpoint)
                .send()
                .get("/cloud")
                .message()
                .type(MessageType.JSON)
                .header("sessionid",salapiClient.getSessionId())
        );

        /**
         * Assert that SAL doesn't have any cloud provider registered
         */
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
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                })
        );
    }
}
