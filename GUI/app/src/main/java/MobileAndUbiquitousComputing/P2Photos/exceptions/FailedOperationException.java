package MobileAndUbiquitousComputing.P2Photos.exceptions;

public class FailedOperationException extends RuntimeException {
    public FailedOperationException() {
        super();
    }

    public FailedOperationException(String msg) {
        super(msg);
    }
}
