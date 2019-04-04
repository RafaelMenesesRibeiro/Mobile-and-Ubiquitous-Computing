package MobileAndUbiquitousComputing.P2Photos.MsgTypes;

public class BasicResponse {
    private int code;
    private String operation;
    private String message;

    public BasicResponse() {}

    public BasicResponse(int code, String message, String operation) {
        this.code = code;
        this.message = message;
        this.operation = operation;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
