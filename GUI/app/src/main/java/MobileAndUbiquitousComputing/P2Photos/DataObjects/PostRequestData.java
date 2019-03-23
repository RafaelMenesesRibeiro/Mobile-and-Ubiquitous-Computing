package MobileAndUbiquitousComputing.P2Photos.DataObjects;

import org.json.JSONObject;

public class PostRequestData extends RequestData {
    private JSONObject params;

    public PostRequestData(RequestType requestType, String url, JSONObject params) {
        super(requestType, url);
        this.params = params;
    }

    public JSONObject getParams() {
        return params;
    }

    public void setParams(JSONObject params) {
        this.params = params;
    }

    @Override
    public String toString() {
        return super.toString() + "\nJSON: " + params.toString();
    }
}
