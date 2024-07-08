package ubi.example.tests;

import io.quarkus.test.junit.QuarkusTest;
import org.citrusframework.TestCaseRunner;
import org.citrusframework.annotations.CitrusEndpoint;
import org.citrusframework.annotations.CitrusResource;
import org.citrusframework.jms.endpoint.JmsEndpoint;
import org.citrusframework.quarkus.CitrusSupport;
import org.junit.jupiter.api.Test;
import org.citrusframework.testng.TestNGCitrusSupport;
import org.testng.Assert;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.citrusframework.actions.ReceiveMessageAction.Builder.receive;
import static org.citrusframework.actions.SendMessageAction.Builder.send;

@QuarkusTest
@CitrusSupport
public class MyIntegrationTests extends TestNGCitrusSupport {

    @CitrusEndpoint
    private JmsEndpoint todoJmsEndpoint;

    @CitrusEndpoint
    private JmsEndpoint todoReportEndpoint;

    @CitrusResource
    TestCaseRunner t;

    @Test
    public void testSendTodoMessage() throws IOException {

        String addTodoEntryPayload = new String(Files.readAllBytes(Paths.get("src/test/resources/mocks/addTodoEntry.json")));

        t.when(send(todoJmsEndpoint)
                .message()
                .header("_type", "JSONObject")
                .body(addTodoEntryPayload));
    }

    @Test
    public void testReceiveMessage() throws IOException {

        String addTodoEntryPayload = new String(Files.readAllBytes(Paths.get("src/test/resources/mocks/addTodoEntry.json")));


        t.then(receive(todoJmsEndpoint)
                .message()
                .header("_type", "JSONObject")
                .body(addTodoEntryPayload));
    }

    @Test
    public void testSendTodoReportMessage() throws IOException {

        String addTodoEntryPayload = new String(Files.readAllBytes(Paths.get("src/test/resources/mocks/reportTodoEntryDone.json")));

        t.when(send(todoReportEndpoint)
                .message()
                .header("_type", "JSONObject")
                .body(addTodoEntryPayload));
    }

    @Test
    public void testReceiveNoBodyMessage() throws IOException {

        String addTodoEntryPayload = new String(Files.readAllBytes(Paths.get("src/test/resources/mocks/reportTodoEntryDone.json")));


        t.then(receive(todoReportEndpoint)
                .message()
                .header("_type", "JSONObject")
                .validate((message, context) -> {
                    // Only validate headers
                    Assert.assertEquals(message.getHeader("_type"), "JSONObject");
                    // Ignore body
                }));
    }
}

