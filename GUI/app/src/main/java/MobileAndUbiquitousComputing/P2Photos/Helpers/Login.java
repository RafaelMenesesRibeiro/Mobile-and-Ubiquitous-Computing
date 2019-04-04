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
import MobileAndUbiquitousComputing.P2Photos.MsgTypes.BasicResponse;
import MobileAndUbiquitousComputing.P2Photos.MsgTypes.SuccessResponse;

public class Login {
    private static final String URL = "http://p2photo-production.herokuapp.com/login";

    private static String username;
    private static String password;
    private static String sessionID;

    public Login(Activity activity, String username, String password) throws FailedLoginException {
        Log.i("MSG", "Logging in as " + username + ".");

        JSONObject json = new JSONObject();
        try {
            json.accumulate("username", username);
            json.accumulate("password", password);
        }
        catch (JSONException jex) {
            jex.printStackTrace();
            throw new FailedLoginException(jex.getMessage());
        }
        RequestData rData = new PostRequestData(activity, RequestData.RequestType.LOGIN, Login.URL, json);
        try {
            ResponseData result = new ExecuteQuery().execute(rData).get();
            int code = result.getServerCode();
            if (code == 200) {
                Log.i("STATUS", "The login operation was successful");

                Login.username = username;
                Login.password = password;
                // The SessionID cookie is stored in getJSONStringFromHttpResponse //
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

    public static String getSessionID() {
        return sessionID;
    }

    public static String getUsername() {
        return username;
    }

    public static String getPassword() {
        return password;
    }
}
