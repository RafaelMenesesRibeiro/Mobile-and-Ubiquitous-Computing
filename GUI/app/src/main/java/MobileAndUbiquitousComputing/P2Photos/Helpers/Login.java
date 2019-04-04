package MobileAndUbiquitousComputing.P2Photos.Helpers;

import android.app.Activity;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.CookieManager;
import java.util.concurrent.ExecutionException;

import MobileAndUbiquitousComputing.P2Photos.DataObjects.PostRequestData;
import MobileAndUbiquitousComputing.P2Photos.DataObjects.RequestData;
import MobileAndUbiquitousComputing.P2Photos.DataObjects.ResponseData;
import MobileAndUbiquitousComputing.P2Photos.Exceptions.FailedLoginException;
import MobileAndUbiquitousComputing.P2Photos.Exceptions.WrongCredentialsException;

public class Login {
    public final static String P2PHOTO_DOMAIN = "p2photo-production.herokuapp.com";
    public final static String P2PHOTO_HOST = "https://p2photo-production.herokuapp.com/";
    public final static CookieManager cookieManager = new CookieManager();
    // TODO IMPLEMENT A COOKIE MANAGER AND A COOKIESTORE

    private static String sessionId;
    private static String username;
    private static String password;

    public static String getSessionId() { return sessionId; }
    public static String getUsername() { return username; }
    public static String getPassword() { return password; }

    // TODO move Login class to Login Activity as login() method, use SessionManager and and ConnectionManager
    // to form everything you need.
    public Login(Activity activity, String username, String password) throws FailedLoginException {
        Log.i("MSG", "Logging in as " + username + ".");
        String url = P2PHOTO_HOST + "login";
        try {
            JSONObject requestBody = new JSONObject();
            requestBody.put("username", username);
            requestBody.put("password", password);
            RequestData requestData = new PostRequestData(RequestData.RequestType.POST, url, requestBody);

            ResponseData result = new QueryManager().execute(requestData).get();
            int code = result.getServerCode();
            if (code == 200) { // TODO Our server already supports ResponseEntities as taught by you.
                Log.i("STATUS", "The login operation was successful");
                Login.username = username;
                Login.password = password;
                //TODO - SessionID.updateSessionID(activity, sessionID);
            }
            else if (code == 401) {
                Log.i("STATUS", "The login operation was unsuccessful. The username or password are incorrect.");
                throw new WrongCredentialsException();
            }
            else {
                Log.i("STATUS", "The login operation was unsuccessful. Unknown error.");
                throw new FailedLoginException();
            }
        }
        catch (JSONException | ExecutionException | InterruptedException exc) {
            exc.printStackTrace();
        }
    }
}
