package cmov1819.p2photo.dataobjects;

import android.app.Activity;

public class RequestData {
    public enum RequestType {
        LOGIN,
        LOGOUT,
        SIGNUP,
        SEARCH_USERS,
        GET_CATALOG_TITLE,
        GET_CATALOG,
        GET_GOOGLE_IDENTIFIERS,
        GET_MEMBERSHIP_CATALOG_IDS,
        GET_MEMBERSHIPS,
        GET_MEMBER_KEY,
        GET_SERVER_LOGS,
        GET_MEMBER_PUBLIC_KEY,
        ASSERT_MEMBERSHIP,
        NEW_CATALOG,
        NEW_CATALOG_SLICE,
        NEW_CATALOG_MEMBER,
        NEW_MEMBER_PUBLIC_KEY
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
