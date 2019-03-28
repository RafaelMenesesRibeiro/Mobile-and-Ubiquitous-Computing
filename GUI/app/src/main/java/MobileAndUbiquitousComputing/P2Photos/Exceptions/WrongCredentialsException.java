package MobileAndUbiquitousComputing.P2Photos.Exceptions;

public class WrongCredentialsException extends FailedLoginException {
    public WrongCredentialsException() {
        super();
    }

    public WrongCredentialsException(String msg) {
        super(msg);
    }
}
