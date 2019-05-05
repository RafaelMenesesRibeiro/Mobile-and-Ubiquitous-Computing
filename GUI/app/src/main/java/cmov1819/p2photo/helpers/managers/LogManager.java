package cmov1819.p2photo.helpers.managers;

import android.util.Log;

import java.text.DateFormat;
import java.util.Date;

import cmov1819.p2photo.dataobjects.RequestData;
import cmov1819.p2photo.dataobjects.ResponseData;

public class LogManager {
    public static final String SIGN_UP_TAG = "Sign Up";
    public static final String LOGIN_TAG = "Login";
    public static final String LOGOUT_TAG = "Logout";
    public static final String ADD_PHOTO_TAG = "Add Photo";
    public static final String LIST_USERS_TAG = "List Users";
    public static final String NEW_CATALOG_TAG = "New Catalog";
    public static final String NEW_CATALOG_MEMBER_TAG = "New Catalog Member";
    public static final String NEW_CATALOG_SLICE_TAG  = "New Catalog Slice";
    public static final String SEARCH_USER_TAG = "Search User";
    public static final String VIEW_CATALOG_TAG = "View Catalog";
    public static final String VIEW_USER_CATALOGS_TAG = "View User Catalogs";
    public static final String GET_CATALOG_TILE_TAG = "Get Catalog Title";
    public static final String GET_CATALOG_TAG = "Get Catalog";
    public static final String GET_MEMBERSHIPS_TAG = "Get Memberships";
    public static final String GET_GOOGLE_IDENTIFIERS_TAG = "Get Google Identifiers";
    public static final String GET_MEMBERSHIP_CATALOG_IDS_TAG = "Get Catalog IDs";
    public static final String GET_SERVER_LOG = "Get Server Log";
    public static final String VIEW_APP_LOG = "View App Log";

    public static final String QUERY_MANAGER_TAG = "Quert Manager";

    private static String logText = "";

    private LogManager() {
        // Does not allow this class to be instantiated. //
    }

    public static String getAppLog() {
        return logText;
    }

    /**********************************************************
     * LOGS FOR OPERATIONS COMPLETION
     ***********************************************************/

    private static void logOperation(String tag, String msg) {
        String currentDateTimeString = DateFormat.getDateTimeInstance().format(new Date());
        String toLog = "\nDate: " + currentDateTimeString + "\n" + msg;
        Log.i(tag, toLog);
        logText += "\n" + toLog;
    }

    public static void logLogout(String username) {
        String msg = "Logout Operation completed. User: " + username;
        logOperation(LOGOUT_TAG, msg);
    }

    public static void logAddPhoto() {
        logOperation(ADD_PHOTO_TAG, "Add Photo Operation completed.");
    }

    public static void logListUsers() {
        logOperation(LIST_USERS_TAG, "List Users Operation completed.");
    }

    public static void logNewCatalog(String catalogID, String catalogTitle) {
        String msg = "New Catalog Operation completed. Created catalog <id, title>: " + catalogID + ", " + catalogTitle;
        logOperation(NEW_CATALOG_TAG, msg);
    }

    public static void logNewCatalogMember(String catalogID, String catalogTitle, String username) {
        String msg = "New Catalog Member Operation completed. Added user: " + username + " to catalog <id, title>: " + catalogID + ", " + catalogTitle;
        logOperation(NEW_CATALOG_MEMBER_TAG, msg);
    }

    public static void logSearchUser(String searchedPattern) {
        String msg = "Search User Operation completed. Searched for pattern: " + searchedPattern;
        logOperation(SEARCH_USER_TAG, msg);
    }

    public static void logViewCatalog(String catalogID, String catalogTitle) {
        String msg = "View Catalog Operation completed. Viewed catalog <id, title>:" + catalogID + ", " + catalogTitle;
        logOperation(VIEW_CATALOG_TAG, msg);
    }

    public static void logViewUserCatalogs(String username) {
        String msg = "View User Catalogs Operation completed. Viewed catalogs for user: " + username;
        logOperation(VIEW_USER_CATALOGS_TAG, msg);
    }

    public static void logViewAppLog() {
        logOperation(VIEW_APP_LOG, "View App Log Operation completed.");
    }

    public static void logGetServerLog() {
        logOperation(GET_SERVER_LOG, "View Server Log Operation completed.");
    }

    /**********************************************************
     * LOGS FOR RECEIVED RESPONSES
     ***********************************************************/

    private static void logReceived(String tag, String msg, ResponseData responseData) {
        String currentDateTimeString = DateFormat.getDateTimeInstance().format(new Date());
        String toLog = "\nDate: " + currentDateTimeString + "\n" + msg + "\n" + responseData.toString();
        Log.i(tag, toLog);
        logText += "\n" + toLog;
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

    public static void logReceivedServerLog(ResponseData responseData) {
        logReceived(GET_SERVER_LOG, "Received Get Server Log response", responseData);
    }

    /**********************************************************
     * LOGS FOR SENT MESSAGES
     ***********************************************************/

    public static void logSentMessage(RequestData requestData) {
        String currentDateTimeString = DateFormat.getDateTimeInstance().format(new Date());
        String toLog = "\nDate: " + currentDateTimeString + "\nStarting request:\n" + requestData.toString();
        Log.i("STATUS", toLog);
        logText += "\n" + toLog;
    }

    /**********************************************************
     * LOGS FOR ERROR MESSAGES
     ***********************************************************/

    public static void logError(String tag, String msg) {
        String currentDateTimeString = DateFormat.getDateTimeInstance().format(new Date());
        String toLog = "\nDate: " + currentDateTimeString + "\n" + msg;
        Log.e(tag, toLog);
        logText += "\n" + toLog;
    }

    /**********************************************************
     * LOGS FOR INFORMATION MESSAGES
     ***********************************************************/

    public static void logInfo(String tag, String msg) {
        String currentDateTimeString = DateFormat.getDateTimeInstance().format(new Date());
        String toLog = "\nDate: " + currentDateTimeString + "\n" + msg;
        Log.i(tag, toLog);
        logText += "\n" + toLog;
    }

    /**********************************************************
     * LOGS FOR WARNING MESSAGES
     ***********************************************************/

    public static void logWarning(String tag, String msg) {
        String currentDateTimeString = DateFormat.getDateTimeInstance().format(new Date());
        String toLog = "\nDate: " + currentDateTimeString + "\n" + msg;
        Log.w(tag, toLog);
        logText += "\n" + toLog;
    }
}
