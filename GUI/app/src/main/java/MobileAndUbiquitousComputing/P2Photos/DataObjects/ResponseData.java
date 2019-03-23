package MobileAndUbiquitousComputing.P2Photos.DataObjects;

public class ResponseData {
    // TODO - Consider removing ResponseCodes if they don't prove useful. //
    public enum ResponseCode {
        SUCCESS,
        UNSUCCESS,
        NO_CODE,
    }

    private int serverCode;
    private ResponseCode responseCode;

    public ResponseData(ResponseCode code, int serverCode) {
        this.responseCode = code;
        this.serverCode = serverCode;
    }

    public ResponseCode getResponseCode() {
        return responseCode;
    }

    public int getServerCode() {
        return serverCode;
    }
}
