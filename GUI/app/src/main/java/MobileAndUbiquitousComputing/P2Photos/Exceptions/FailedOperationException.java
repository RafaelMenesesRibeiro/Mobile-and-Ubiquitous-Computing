package MobileAndUbiquitousComputing.P2Photos.Exceptions;

public class FailedOperationException extends RuntimeException {
    public FailedOperationException() {
        super();
    }

    public FailedOperationException(String msg) {
        super(msg);
    }
}
