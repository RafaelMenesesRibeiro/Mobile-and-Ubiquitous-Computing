package MobileAndUbiquitousComputing.P2Photos.Helpers;

import android.app.Activity;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.ExecutionException;

import MobileAndUbiquitousComputing.P2Photos.DataObjects.PostRequestData;
import MobileAndUbiquitousComputing.P2Photos.DataObjects.RequestData;
import MobileAndUbiquitousComputing.P2Photos.DataObjects.ResponseData;
import MobileAndUbiquitousComputing.P2Photos.Exceptions.FailedLoginException;
import MobileAndUbiquitousComputing.P2Photos.Exceptions.WrongCredentialsException;

public class Login {
    private static String sessionID;
    private static String username;

    private static String password;

    public static String getSessionID() { return sessionID; }
    public static String getUsername() { return username; }
    public static String getPassword() { return password; }

    public Login(Activity activity, String username, String password) throws FailedLoginException {
        Log.i("MSG", "Logging in as " + username + ".");
        String url = "http://p2photo-production.herokuapp.com/login";
        JSONObject json = new JSONObject();
        try {
            json.accumulate("username", username);
            json.accumulate("password", password);
        }
        catch (JSONException jex) {
            jex.printStackTrace();
        }
        RequestData rData = new PostRequestData(RequestData.RequestType.POST, url, json);
        try {
            ResponseData result = new ExecuteQuery().execute(rData).get();
            int code = result.getServerCode();
            if (code == 200) {
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
        catch (ExecutionException eex) {
            eex.printStackTrace();
        }
        catch (InterruptedException iex) {
            iex.printStackTrace();
        }
    }
}
