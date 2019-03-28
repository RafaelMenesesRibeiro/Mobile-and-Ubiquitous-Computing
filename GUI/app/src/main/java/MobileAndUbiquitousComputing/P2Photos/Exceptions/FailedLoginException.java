package MobileAndUbiquitousComputing.P2Photos.Exceptions;

public class FailedLoginException extends RuntimeException {
    public FailedLoginException() {
        super();
    }

    public FailedLoginException(String msg) {
        super(msg);
    }
}
