package cmov1819.p2photo.dataobjects;

import android.app.Activity;

import org.json.JSONException;
import org.json.JSONObject;

import static cmov1819.p2photo.helpers.termite.Consts.RID;
import static cmov1819.p2photo.helpers.termite.Consts.TIMESTAMP;

public class PostRequestData extends RequestData {
    private JSONObject params;

    public PostRequestData(Activity activity, RequestType requestType, String url, JSONObject params) {
        super(activity, requestType, url);
        this.params = params;
    }

    public JSONObject getParams() {
        return params;
    }

    public void setParams(JSONObject params) {
        this.params = params;
    }

    public void addTimestamp(String timestamp) {
        try {
            params.put(TIMESTAMP, timestamp);
        }
        catch (JSONException ex) {
            // Ignores it, the server will return the appropriate response.
            // It is better to request the server than to try and stop the request.
            // Less error prone and more easily read and maintained.
        }
    }

    public void addRequestID(int requestID) {
        try { params.put(RID, requestID); }
        catch (JSONException ex) { /* Ignores it. */ }
    }

    @Override
    public String toString() {
        return super.toString() + "\nJSON: " + params.toString();
    }
}
