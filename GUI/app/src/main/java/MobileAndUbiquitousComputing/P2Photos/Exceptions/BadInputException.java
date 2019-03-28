package MobileAndUbiquitousComputing.P2Photos.Exceptions;

public class BadInputException extends RuntimeException {
    public BadInputException() {
        super();
    }

    public BadInputException(String msg) {
        super(msg);
    }
}
