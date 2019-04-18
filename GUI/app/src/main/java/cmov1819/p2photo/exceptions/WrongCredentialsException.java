package cmov1819.p2photo.exceptions;

public class WrongCredentialsException extends FailedLoginException {
    public WrongCredentialsException() {
        super();
    }

    public WrongCredentialsException(String msg) {
        super(msg);
    }
}
