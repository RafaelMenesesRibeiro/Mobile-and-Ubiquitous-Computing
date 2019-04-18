package cmov1819.p2photo.dataobjects;

import cmov1819.p2photo.msgtypes.BasicResponse;

public class ResponseData {
    private int serverCode;
    private BasicResponse payload;

    public ResponseData(int serverCode, BasicResponse payload) {
        this.serverCode = serverCode;
        this.payload = payload;
    }

    public int getServerCode() {
        return serverCode;
    }

    public BasicResponse getPayload() {
        return payload;
    }

    @Override
    public String toString() {
        return "Server code: " + serverCode + ".\n" + payload.toString();
    }
}
