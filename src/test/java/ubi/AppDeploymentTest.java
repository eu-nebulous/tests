package ubi;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import org.citrusframework.TestCaseRunner;
import org.citrusframework.annotations.CitrusEndpoint;
import org.citrusframework.annotations.CitrusResource;
import org.citrusframework.context.TestContext;
import org.citrusframework.jms.endpoint.JmsEndpoint;
import org.citrusframework.quarkus.CitrusSupport;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ubi.util.FileTemplatingUtils;

import java.text.SimpleDateFormat;
import java.util.*;

import static org.citrusframework.actions.SendMessageAction.Builder.send;
import static org.citrusframework.dsl.JsonSupport.marshal;

@QuarkusTest
@CitrusSupport
public class AppDeploymentTest {
    private static final Logger log = Logger.getLogger(AppDeploymentTest.class);

    private final Config config = ConfigProvider.getConfig();
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

    @CitrusResource
    TestCaseRunner t;

    @CitrusResource
    private TestContext context;

    @BeforeEach
    public void setup() {
        context.getReferenceResolver().bind("objectMapper", objectMapper);
    }

    @Test
    void test() throws Exception {
        log.info("send app creation message");

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
        appParameters.put("{{APP_EMS_USER}}", Optional.ofNullable(config.getValue("app.ems.username", String.class)).orElseThrow(() -> new IllegalStateException("APP_EMS_USER env var is not defined")));
        appParameters.put("{{APP_EMS_PASSWORD}}", Optional.ofNullable(config.getValue("app.ems.password", String.class)).orElseThrow(() -> new IllegalStateException("APP_EMS_PASSWORD env var is not defined")));

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
        envVars.add(Map.of("name", "PRIVATE_DOCKER_REGISTRY_SERVER", "value", Optional.ofNullable(config.getValue("docker.server", String.class)).orElseThrow(() -> new IllegalStateException("MQTT_APP_PRIVATE_DOCKER_REGISTRY_SERVER env var is not defined")), "secret", "false"));
        envVars.add(Map.of("name", "PRIVATE_DOCKER_REGISTRY_USERNAME", "value", Optional.ofNullable(config.getValue("docker.username", String.class)).orElseThrow(() -> new IllegalStateException("MQTT_APP_PRIVATE_DOCKER_REGISTRY_USERNAME env var is not defined")), "secret", "false"));
        envVars.add(Map.of("name", "PRIVATE_DOCKER_REGISTRY_PASSWORD", "value", Optional.ofNullable(config.getValue("docker.password", String.class)).orElseThrow(() -> new IllegalStateException("MQTT_APP_PRIVATE_DOCKER_REGISTRY_PASSWORD env var is not defined")), "secret", "false"));
        envVars.add(Map.of("name", "PRIVATE_DOCKER_REGISTRY_EMAIL", "value", Optional.ofNullable(config.getValue("docker.email", String.class)).orElseThrow(() -> new IllegalStateException("MQTT_APP_PRIVATE_DOCKER_REGISTRY_EMAIL env var is not defined")), "secret", "false"));
        envVars.add(Map.of("name", "ONM_URL", "value", Optional.ofNullable(config.getValue("onm_url", String.class)).orElseThrow(() -> new IllegalStateException("ONM_URL env var is not defined"))));

        log.info(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(appCreationPayload));

        t.when(send(appCreationEndpoint)
                .message()
                .header("applicationId", applicationId)
                .body(marshal(appCreationPayload)));
    }
}
