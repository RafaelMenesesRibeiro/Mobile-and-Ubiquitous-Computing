package MobileAndUbiquitousComputing.P2Photos.Exceptions;

public class NoResultsException extends RuntimeException {
    public NoResultsException() {
        super();
    }

    public NoResultsException(String msg) {
        super(msg);
    }
}
