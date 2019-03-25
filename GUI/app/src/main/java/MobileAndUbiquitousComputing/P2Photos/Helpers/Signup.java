package MobileAndUbiquitousComputing.P2Photos.Helpers;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.ExecutionException;

import MobileAndUbiquitousComputing.P2Photos.DataObjects.PostRequestData;
import MobileAndUbiquitousComputing.P2Photos.DataObjects.RequestData;
import MobileAndUbiquitousComputing.P2Photos.DataObjects.ResponseData;

public class Signup {

    public static boolean SignupUser(String username, String password) {
        Log.i("MSG", "Signing up in as " + username + ".");
        String url = "http://p2photo-production.herokuapp.com/signup";
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
                Log.i("STATUS", "The sign up operation was successful");
            }
            else if (code == 422) {
                Log.i("STATUS", "The sign up operation was unsuccessful. The username chosen already exists.");
            }
            else {
                Log.i("STATUS", "The sign up operation was unsuccessful. Unknown error.");
            }
        }
        catch (ExecutionException eex) {
            eex.printStackTrace();
        }
        catch (InterruptedException iex) {
            iex.printStackTrace();
        }

        Login login = new Login(username, password);
        String sessionID = "sessionID";
        login.LoginUser(sessionID);
        // Returns true if successful, false otherwise.
        return true;
    }
}
