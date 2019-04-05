package MobileAndUbiquitousComputing.P2Photos.msgtypes;

import MobileAndUbiquitousComputing.P2Photos.msgtypes.BasicResponse;

public class SuccessResponse extends BasicResponse {
    private Object result;

    public SuccessResponse(int code, String message, String operation, Object result) {
        super(code, message, operation);
        this.result = result;
    }

    public SuccessResponse() {}

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }

    @Override
    public String toString() {
        return "SuccessResponse{" +
                "result=" + result +
                '}';
    }
}
