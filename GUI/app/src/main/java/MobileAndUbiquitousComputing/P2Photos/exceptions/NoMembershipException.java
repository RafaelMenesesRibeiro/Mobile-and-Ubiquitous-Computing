package MobileAndUbiquitousComputing.P2Photos.exceptions;

public class NoMembershipException extends RuntimeException {
    public NoMembershipException() {
        super();
    }

    public NoMembershipException(String msg) {
        super(msg);
    }
}
