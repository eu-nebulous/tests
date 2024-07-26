package ubi.tests;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.citrusframework.TestCaseRunner;
import org.citrusframework.annotations.CitrusEndpoint;
import org.citrusframework.annotations.CitrusResource;
import org.citrusframework.context.TestContext;
import org.citrusframework.jms.endpoint.JmsEndpoint;
import org.citrusframework.testng.spring.TestNGCitrusSpringSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import ubi.util.FileTemplatingUtils;

import java.text.SimpleDateFormat;
import java.util.*;

import static org.citrusframework.actions.ReceiveMessageAction.Builder.receive;
import static org.citrusframework.actions.SendMessageAction.Builder.send;
import static org.citrusframework.dsl.JsonSupport.marshal;

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

    @CitrusEndpoint
    private JmsEndpoint appCreationEndpoint;

    @CitrusEndpoint
    private JmsEndpoint metricModelEndpoint;

    @CitrusEndpoint
    private JmsEndpoint evaluatorEndpoint;

    @CitrusResource
    private TestCaseRunner t;

    @CitrusResource
    private TestContext context;

    @BeforeClass
    public void setup() {
        context.getReferenceResolver().bind("objectMapper", objectMapper);
    }

    @Test
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
        appParameters.put("{{APP_EMS_USER}}", "ems_user"); // Set appropriate value
        appParameters.put("{{APP_EMS_PASSWORD}}", "ems_password"); // Set appropriate value

        Map<String, Object> appCreationPayload = FileTemplatingUtils
                .loadJSONFileAndSubstitute("mqtt_processor_app/app_creation_message.json", appParameters);
        ArrayList<Object> envVars = ((ArrayList<Object>) appCreationPayload.get("environmentVariables"));

        appCreationPayload.put("content",
                FileTemplatingUtils.loadFileAndSubstitute("mqtt_processor_app/kubevela.yaml", appParameters));

        // Configure cloud id
        ArrayList<Object> resources = ((ArrayList<Object>) appCreationPayload.get("resources"));
        resources.clear();
        resources.add(Map.of("uuid", "aws-automated-testing", "title", "", "platform", "", "enabled", "true", "regions", "us-east-1"));

        // Configure docker registry
        envVars.add(Map.of("name", "PRIVATE_DOCKER_REGISTRY_SERVER", "value", "docker_server")); // Set appropriate value
        envVars.add(Map.of("name", "PRIVATE_DOCKER_REGISTRY_USERNAME", "value", "docker_username")); // Set appropriate value
        envVars.add(Map.of("name", "PRIVATE_DOCKER_REGISTRY_PASSWORD", "value", "docker_password")); // Set appropriate value
        envVars.add(Map.of("name", "PRIVATE_DOCKER_REGISTRY_EMAIL", "value", "docker_email")); // Set appropriate value
        envVars.add(Map.of("name", "ONM_URL", "value", "onm_url_value")); // Set appropriate value

        t.when(send(appCreationEndpoint)
                .message()
                .header("application", applicationId)
                .body(marshal(appCreationPayload)));

        /**
         * Send metric model and assert is correctly received by any subscriber
         */
        Map<String, Object> metricModelPayload = FileTemplatingUtils.loadJSONFileAndSubstitute("mqtt_processor_app/metric_model.json",
                Map.of("{{APP_ID}}", applicationId));

        t.when(send(metricModelEndpoint)
                .message()
                .header("application", applicationId)
                .body(marshal(metricModelPayload)));

        t.then(receive(metricModelEndpoint)
                .message()
                .header("application", applicationId));
    }
}
