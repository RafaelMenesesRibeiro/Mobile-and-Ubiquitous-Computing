package MobileAndUbiquitousComputing.P2Photos.exceptions;

public class WrongCredentialsException extends FailedLoginException {
    public WrongCredentialsException() {
        super();
    }

    public WrongCredentialsException(String msg) {
        super(msg);
    }
}
