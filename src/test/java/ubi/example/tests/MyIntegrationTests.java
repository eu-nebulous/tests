package ubi.example.tests;

import org.citrusframework.annotations.CitrusTest;
import org.citrusframework.jms.endpoint.JmsEndpoint;
import org.citrusframework.testng.spring.TestNGCitrusSpringSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.testng.annotations.Test;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.IOException;

import static org.citrusframework.actions.SendMessageAction.Builder.send;
import static org.citrusframework.actions.ReceiveMessageAction.Builder.receive;
import static org.testng.AssertJUnit.assertEquals;

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
                .body(addTodoEntryPayload));


        $(receive()
                .endpoint(todoJmsEndpoint)
                .message()
                .header("_type", "JSONObject")
                .validate((message, context) -> {
                    // Only validate headers
                    assertEquals(message.getHeader("_type"), "JSONObject");
                    // Ignore body
                }));


    }

//    @Test
//    @CitrusTest
//    public void testReceiveMessage() throws IOException {
//        String addTodoEntryPayload = new String(Files.readAllBytes(Paths.get("src/test/resources/mocks/addTodoEntry.json")));
//
//        $(receive()
//                .endpoint(todoJmsEndpoint)
//                .message()
//                .header("_type", "JSONObject")
//                .body(addTodoEntryPayload));
//    }
//
//    @Test
//    @CitrusTest
//    public void testSendTodoReportMessage() throws IOException {
//        String reportTodoEntryDonePayload = new String(Files.readAllBytes(Paths.get("src/test/resources/mocks/reportTodoEntryDone.json")));
//
//        $(send()
//                .endpoint(todoReportEndpoint)
//                .message()
//                .header("_type", "JSONObject")
//                .body(reportTodoEntryDonePayload));
//    }
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
