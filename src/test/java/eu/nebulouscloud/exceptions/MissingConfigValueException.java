package eu.nebulouscloud.exceptions;

public class MissingConfigValueException extends IllegalStateException{
    public MissingConfigValueException(String s) {
        super("The required configuration property '" + s + "' is missing or not defined in application.properties.");
    }
}
