package MobileAndUbiquitousComputing.P2Photos.Exceptions;

public class UsernameExistsException extends RuntimeException {
    public UsernameExistsException() {
        super();
    }

    public UsernameExistsException(String msg) {
        super(msg);
    }
}
