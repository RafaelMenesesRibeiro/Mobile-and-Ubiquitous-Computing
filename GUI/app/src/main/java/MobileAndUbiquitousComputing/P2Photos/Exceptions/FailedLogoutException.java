package MobileAndUbiquitousComputing.P2Photos.Exceptions;

public class FailedLogoutException extends RuntimeException{
    public FailedLogoutException() {
        super();
    }

    public FailedLogoutException(String msg) {
        super(msg);
    }
}
