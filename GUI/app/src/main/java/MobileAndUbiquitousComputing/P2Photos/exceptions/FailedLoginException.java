package MobileAndUbiquitousComputing.P2Photos.exceptions;

public class FailedLoginException extends RuntimeException {
    public FailedLoginException() {
        super();
    }

    public FailedLoginException(String msg) {
        super(msg);
    }
}
