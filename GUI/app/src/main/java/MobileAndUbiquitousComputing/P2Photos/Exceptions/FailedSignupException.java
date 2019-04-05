package MobileAndUbiquitousComputing.P2Photos.Exceptions;

public class FailedSignupException extends RuntimeException {
    public FailedSignupException() {
        super();
    }

    public FailedSignupException(String msg) {
        super(msg);
    }
}
