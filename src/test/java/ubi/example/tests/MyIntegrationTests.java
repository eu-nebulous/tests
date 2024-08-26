package ubi.example.tests;

import org.citrusframework.annotations.CitrusTest;
import org.citrusframework.jms.endpoint.JmsEndpoint;
import org.citrusframework.message.MessageType;
import org.citrusframework.testng.spring.TestNGCitrusSpringSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;
import ubi.example.config.EndpointConfig;
import ubi.util.FileTemplatingUtils;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.citrusframework.actions.SendMessageAction.Builder.send;
import static org.citrusframework.actions.ReceiveMessageAction.Builder.receive;
import static org.citrusframework.dsl.JsonSupport.marshal;
import static org.testng.AssertJUnit.assertEquals;

@ContextConfiguration(classes = {EndpointConfig.class})
public class MyIntegrationTests extends TestNGCitrusSpringSupport {

    @Autowired
    @Qualifier("todoJmsEndpoint")
    private JmsEndpoint todoJmsEndpoint;

    @Autowired
    @Qualifier("todoReportEndpoint")
    private JmsEndpoint todoReportEndpoint;

    @Test
    @CitrusTest
    public void testSendTodoMessage() throws IOException {
        String addTodoEntryPayload = new String(Files.readAllBytes(Paths.get("src/test/resources/mocks/addTodoEntry.json")));

        $(send()
                .endpoint(todoJmsEndpoint)
                .message()
                .header("_type", "JSONObject")
                .header("application", "2")
                .header("subject", "2")
                .body(marshal(addTodoEntryPayload)));


//        $(receive()
//                .endpoint(todoJmsEndpoint)
//                .message()
//                .header("_type", "JSONObject")
//                .validate((message, context) -> {
//                    // Only validate headers
//                    assertEquals(message.getHeader("_type"), "JSONObject");
//                    // Ignore body
//                }));


    }

    @Test
    @CitrusTest
    public void testReceiveMessage() throws IOException {
        String addTodoEntryPayload = new String(Files.readAllBytes(Paths.get("src/test/resources/mocks/addTodoEntry.json")));

        Map<String, String> selectorMap = new HashMap<>();
        selectorMap.put("application", "2");
        selectorMap.put("subject", "2");

        $(receive()
                .endpoint(todoJmsEndpoint)
                .message()
                .selector(selectorMap)
                .header("_type", "JSONObject")
                .validate((message, context) -> {
                    // Only validate headers
                    assertEquals(message.getHeader("_type"), "JSONObject");

                    System.out.println(message.getPayload().toString());
                    // Ignore body
                }));
    }

    @Test
    @CitrusTest
    public void testMapMessage() throws Exception {
        String reportTodoEntryDonePayload = new String(Files.readAllBytes(Paths.get("src/test/resources/mocks/reportTodoEntryDone.json")));

        Map<String, Object> metricModelPayload = FileTemplatingUtils.loadJSONFileAndSubstitute("mqtt_processor_app/metric_model.json",
                Map.of("{{APP_ID}}", "test"));

        $(send()
                .endpoint(todoReportEndpoint)
                .message()
//                .header("_type", "JSONObject")
//                .type(MessageType.JSON)
                .body(metricModelPayload.toString()));

        $(receive()
                .endpoint(todoReportEndpoint)
                .message()
                .validate((message, context) -> {
                    // Only validate headers

                    System.out.println(message);
                    // Ignore body
                }));
    }
//
//    @Test
//    @CitrusTest
//    public void testReceiveNoBodyMessage() throws IOException {
//        String reportTodoEntryDonePayload = new String(Files.readAllBytes(Paths.get("src/test/resources/mocks/reportTodoEntryDone.json")));
//
//        $(receive()
//                .endpoint(todoReportEndpoint)
//                .message()
//                .header("_type", "JSONObject")
//                .validate((message, context) -> {
//                    // Only validate headers
//                    assertEquals(message.getHeader("_type"), "JSONObject");
//                    // Ignore body
//                }));
//    }
}
