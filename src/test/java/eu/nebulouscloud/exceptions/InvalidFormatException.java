package eu.nebulouscloud.exceptions;

public class InvalidFormatException extends Exception {
    public InvalidFormatException(String string) {
        super(string);
    }

    public InvalidFormatException(String string, Throwable cause) {
        super(string, cause);
    }
}
