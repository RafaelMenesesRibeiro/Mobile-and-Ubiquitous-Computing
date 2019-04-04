package MobileAndUbiquitousComputing.P2Photos.Helpers;

import android.app.Activity;
import android.util.Log;

import java.util.concurrent.ExecutionException;

import MobileAndUbiquitousComputing.P2Photos.DataObjects.RequestData;
import MobileAndUbiquitousComputing.P2Photos.DataObjects.ResponseData;

public class Logout {

    public static void LogoutUser(Activity activity) {
        String username = Login.getUsername();
        Log.i("MSG", "Logging in as " + username + ".");
        String url = "http://p2photo-production.herokuapp.com/logout/" + username;
        RequestData rData = new RequestData(activity, RequestData.RequestType.DELETE, url);
        try {
            ResponseData result = new QueryManager().execute(rData).get();
            //TODO - Implement return code handling.

            SessionID.deleteSessionID(activity);
        }
        catch (ExecutionException eex) {
            eex.printStackTrace();
        }
        catch (InterruptedException iex) {
            iex.printStackTrace();
        }
    }
}
