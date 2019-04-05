package MobileAndUbiquitousComputing.P2Photos.DataObjects;

import android.app.Activity;

public class RequestData {
    public enum RequestType {
        LOGIN,
        LOGOUT,
        SIGNUP,
        SEARCH_USERS,
        GET_CATALOG_TITLE,
        GET_CATALOG,
        NEW_ALBUM
    }

    private Activity activity;
    private RequestType requestType;
    private String url;

    public RequestData(Activity activity, RequestType requestType, String url) {
        this.activity = activity;
        this.requestType = requestType;
        this.url = url;
    }

    public Activity getActivity() {
        return activity;
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
