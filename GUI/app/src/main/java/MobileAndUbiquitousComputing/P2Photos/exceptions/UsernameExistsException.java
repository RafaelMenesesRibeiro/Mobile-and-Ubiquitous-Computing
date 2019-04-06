package MobileAndUbiquitousComputing.P2Photos.exceptions;

public class UsernameExistsException extends RuntimeException {
    public UsernameExistsException() {
        super();
    }

    public UsernameExistsException(String msg) {
        super(msg);
    }
}
