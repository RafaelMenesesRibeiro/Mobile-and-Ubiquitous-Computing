package MobileAndUbiquitousComputing.P2Photos.DataObjects;

public class RequestData {
    public enum RequestType {POST, GET, PUT}

    private RequestType requestType;
    private String url;

    public RequestData(RequestType requestType, String url) {
        this.requestType = requestType;
        this.url = url;
    }

    public RequestType getRequestType() {
        return requestType;
    }

    public String getUrl() {
        return url;
    }

    @Override
    public String toString() {
        return "URL: " + url;
    }
}
