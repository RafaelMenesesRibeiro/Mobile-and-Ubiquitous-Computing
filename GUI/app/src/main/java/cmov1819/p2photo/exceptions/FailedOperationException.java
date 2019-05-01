package cmov1819.p2photo.exceptions;

public class FailedOperationException extends RuntimeException {
    public FailedOperationException() {
        super();
    }

    public FailedOperationException(String msg) {
        super(msg);
    }
}
