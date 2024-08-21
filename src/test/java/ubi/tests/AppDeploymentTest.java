package ubi.tests;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.citrusframework.annotations.CitrusTest;
import org.citrusframework.jms.endpoint.JmsEndpoint;
import org.citrusframework.message.MessageType;
import org.citrusframework.testng.spring.TestNGCitrusSpringSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import ubi.util.FileTemplatingUtils;

import java.text.SimpleDateFormat;
import java.util.*;

import static org.citrusframework.actions.ReceiveMessageAction.Builder.receive;
import static org.citrusframework.actions.SendMessageAction.Builder.send;
import static org.citrusframework.dsl.JsonSupport.marshal;
import static org.testng.AssertJUnit.assertEquals;

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
        resources.add(Map.of("uuid", "c9a625c7-f705-4128-948f-6b5765509029", "title", "blah", "platform", "AWS", "enabled", "true","regions","us-east-1"));

        // Configure docker registry
        envVars.add(Map.of("name", "PRIVATE_DOCKER_REGISTRY_SERVER", "value", env.getProperty("docker.server"),"secret","false"));
        envVars.add(Map.of("name", "PRIVATE_DOCKER_REGISTRY_USERNAME", "value", env.getProperty("docker.username"),"secret","false"));
        envVars.add(Map.of("name", "PRIVATE_DOCKER_REGISTRY_PASSWORD", "value", env.getProperty("docker.password"),"secret","false"));
        envVars.add(Map.of("name", "PRIVATE_DOCKER_REGISTRY_EMAIL", "value", env.getProperty("docker.email"),"secret","false"));
        envVars.add(Map.of("name", "ONM_URL", "value", env.getProperty("onm_url"),"secret","false"));

        /**
         * Header Selectors for receiving published message
         */
        Map<String, String> selectorMap = new HashMap<>();
        selectorMap.put("application", applicationId);
        selectorMap.put("subject", applicationId);
        logger.info(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(appCreationPayload));


        $(send(appCreationEndpoint)
                .message()
                .header("application", applicationId)
                .header("subject", applicationId)
                .header("_type", Map.class)
                .body(marshal(appCreationPayload,objectMapper))
//                .body(appCreationPayload.toString())
        );

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
        $(send(metricModelEndpoint)
                .message()
                .header("application", applicationId)
                .header("subject", applicationId)
                .header("_type", Map.class)
                .body(marshal(metricModelPayload,objectMapper))
//                .body(metricModelPayload.toString())
        );

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
                .timeout(10000)
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
                .timeout(3000)
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
}
