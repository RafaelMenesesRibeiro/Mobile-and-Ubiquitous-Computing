package cmov1819.p2photo.exceptions;

public class UsernameExistsException extends RuntimeException {
    public UsernameExistsException() {
        super();
    }

    public UsernameExistsException(String msg) {
        super(msg);
    }
}
