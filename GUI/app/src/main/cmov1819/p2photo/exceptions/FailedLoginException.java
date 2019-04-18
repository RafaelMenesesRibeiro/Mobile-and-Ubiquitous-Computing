package cmov1819.p2photo.exceptions;

public class FailedLoginException extends RuntimeException {
    public FailedLoginException() {
        super();
    }

    public FailedLoginException(String msg) {
        super(msg);
    }
}
