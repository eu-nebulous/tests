package ubi.example.config;

import jakarta.enterprise.context.ApplicationScoped;
import org.citrusframework.context.TestContext;
import org.citrusframework.exceptions.ValidationException;
import org.citrusframework.message.Message;
import org.citrusframework.validation.context.ValidationContext;
import org.citrusframework.validation.MessageValidator;

import java.util.List;

@ApplicationScoped
public class IgnoreBodyMessageValidator implements MessageValidator<ValidationContext> {

    @Override
    public void validateMessage(Message message, Message message1, TestContext testContext, List<ValidationContext> list) throws ValidationException {

    }

    @Override
    public boolean supportsMessageType(String messageType, Message message) {
        return true;
    }
}
