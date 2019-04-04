package MobileAndUbiquitousComputing.P2Photos.MsgTypes;

public class SuccessResponse extends BasicResponse {
    private Object result;

    public SuccessResponse() {}

    public SuccessResponse(int code, String message, String operation, Object result) {
        super(code, message, operation);
        this.result = result;
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }
}
