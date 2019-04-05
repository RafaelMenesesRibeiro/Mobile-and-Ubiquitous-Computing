package MobileAndUbiquitousComputing.P2Photos.Exceptions;

public class NoMembershipException extends RuntimeException {
    public NoMembershipException() {
        super();
    }

    public NoMembershipException(String msg) {
        super(msg);
    }
}
