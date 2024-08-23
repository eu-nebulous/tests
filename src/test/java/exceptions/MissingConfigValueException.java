package exceptions;

public class MissingConfigValueException extends IllegalStateException{
    public MissingConfigValueException(String s) {
        super(s + "is not defined in application.properties");
    }
}
