package cmov1819.p2photo.helpers.managers;

import android.util.Log;

import java.text.DateFormat;
import java.util.Date;

import cmov1819.p2photo.dataobjects.RequestData;
import cmov1819.p2photo.dataobjects.ResponseData;

public class LogManager {
    private static final String SIGN_UP_TAG = "Sign Up";
    private static final String LOGIN_TAG = "Login";
    private static final String LOGOUT_TAG = "Logout";
    private static final String ADD_PHOTO_TAG = "Add Photo";
    private static final String LIST_USERS_TAG = "List Users";
    private static final String NEW_CATALOG_TAG = "New Catalog";
    private static final String NEW_CATALOG_MEMBER_TAG = "New Catalog Member";
    private static final String NEW_CATALOG_SLICE_TAG  = "New Catalog Slice";
    private static final String SEARCH_USER_TAG = "Search User";
    private static final String VIEW_CATALOG_TAG = "View Catalog";
    private static final String VIEW_USER_CATALOGS_TAG = "View User Catalogs";
    private static final String GET_CATALOG_TILE_TAG = "Get Catalog Title";
    private static final String GET_CATALOG_TAG = "Get Catalog";
    private static final String GET_MEMBERSHIPS_TAG = "Get Memberships";
    private static final String GET_GOOGLE_IDENTIFIERS_TAG = "Get Google Identifiers";
    private static final String GET_MEMBERSHIP_CATALOG_IDS_TAG = "Get Catalog IDs";


    /**********************************************************
     * LOGS FOR OPERATIONS COMPLETION
     ***********************************************************/

    public static void LogAddPhoto() {
        Log.i(ADD_PHOTO_TAG, "Add Photo Operation completed.");
    }

    public static void LogListUsers() {
        Log.i(LIST_USERS_TAG, "List Users Operation completed.");
    }

    public static void LogNewCatalog(String catalogID, String catalogTitle) {
        Log.i(NEW_CATALOG_TAG, "New Catalog Operation completed. Created catalog <id, title>: " + catalogID + ", " + catalogTitle);
    }

    public static void LogNewCatalogMember(String catalogID, String catalogTitle, String username) {
        Log.i(NEW_CATALOG_MEMBER_TAG, "New Catalog Member Operation completed. Added user: "
                + username + " to catalog <id, title>: " + catalogID + ", " + catalogTitle);
    }

    public static void LogSearchUser(String searchedPattern) {
        Log.i(SEARCH_USER_TAG, "Search User Operation completed. Searched for pattern: " + searchedPattern);
    }

    public static void LogViewCatalog(String catalogID, String catalogTitle) {
        Log.i(VIEW_CATALOG_TAG, "View Catalog Operation completed. Viewed catalog <id, title>:" + catalogID + ", " + catalogTitle);
    }

    public static void LogViewUserCatalogs(String username) {
        Log.i(VIEW_USER_CATALOGS_TAG, "View User Catalogs Operation completed. Viewed catalogs for user: " + username);
    }

    /**********************************************************
     * LOGS FOR RECEIVED RESPONSES
     ***********************************************************/

    private static void logReceived(String tag, String msg, ResponseData responseData) {
        String currentDateTimeString = DateFormat.getDateTimeInstance().format(new Date());
        Log.i(tag, "\nDate: " + currentDateTimeString + "\n" + msg + "\n" + responseData.toString());
    }

    public static void logReceivedSignup(ResponseData responseData) {
        logReceived(SIGN_UP_TAG, "Received Sign Up response", responseData);
    }

    public static void logReceivedLogin(ResponseData responseData) {
        logReceived(LOGIN_TAG, "Received Login response", responseData);
    }

    public static void logReceivedLogout(ResponseData responseData) {
        logReceived(LOGOUT_TAG, "Received Logout response", responseData);
    }

    public static void logReceivedSearhUser(ResponseData responseData) {
        logReceived(SEARCH_USER_TAG, "Received Searh User response", responseData);
    }

    public static void logReceivedGetCatalogTitle(ResponseData responseData) {
        logReceived(GET_CATALOG_TILE_TAG, "Received Get Catalog Title response", responseData);
    }

    public static void logReceivedGetCatalog(ResponseData responseData) {
        logReceived(GET_CATALOG_TAG, "Received Get Catalog response", responseData);
    }

    public static void logReceivedNewCatalog(ResponseData responseData) {
        logReceived(NEW_CATALOG_TAG, "Received New Catalog response", responseData);
    }

    public static void logReceivedNewCatalogSlice(ResponseData responseData) {
        logReceived(NEW_CATALOG_SLICE_TAG, "Received New Catalog Slice response", responseData);
    }

    public static void logReceivedNewCatalogMember(ResponseData responseData) {
        logReceived(NEW_CATALOG_MEMBER_TAG, "Received New Catalog Member response", responseData);
    }

    public static void logReceivedGetMemberships(ResponseData responseData) {
        logReceived(GET_MEMBERSHIPS_TAG, "Received Get Memberships response", responseData);
    }

    public static void logReceivedGetGoogleIdentifiers(ResponseData responseData) {
        logReceived(GET_GOOGLE_IDENTIFIERS_TAG, "Received Get Google Identifiers response", responseData);
    }

    public static void logReceivedGetMembershipCatalogIDs(ResponseData responseData) {
        logReceived(GET_MEMBERSHIP_CATALOG_IDS_TAG, "Received Get Membership Catalog IDs response", responseData);
    }

    /**********************************************************
     * LOGS FOR SENT MESSAGES
     ***********************************************************/

    public static void logSentMessage(RequestData requestData) {
        String currentDateTimeString = DateFormat.getDateTimeInstance().format(new Date());
        String msg = "Starting request:";
        Log.i("STATUS", "\nDate: " + currentDateTimeString + "\n" + msg + "\n" + requestData.toString());
    }
}
