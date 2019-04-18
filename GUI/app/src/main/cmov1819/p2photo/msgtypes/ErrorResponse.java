package cmov1819.p2photo.msgtypes;

public class ErrorResponse extends BasicResponse {
    private String reason;

    public ErrorResponse(int code, String message, String operation, String reason) {
        super(code, message, operation);
        this.reason = reason;
    }

    public ErrorResponse() {
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
