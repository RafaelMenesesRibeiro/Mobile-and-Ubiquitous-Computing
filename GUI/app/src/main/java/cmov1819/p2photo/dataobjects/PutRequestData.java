package cmov1819.p2photo.dataobjects;

import android.app.Activity;

import org.json.JSONObject;

public class PutRequestData extends RequestData {
    private JSONObject params;

    public PutRequestData(Activity activity, RequestType requestType, String url, JSONObject params) {
        super(activity, requestType, url);
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
