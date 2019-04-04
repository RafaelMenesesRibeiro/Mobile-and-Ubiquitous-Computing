package MobileAndUbiquitousComputing.P2Photos.Helpers;

import android.app.Activity;
import android.util.Log;

import java.util.concurrent.ExecutionException;

import MobileAndUbiquitousComputing.P2Photos.DataObjects.RequestData;
import MobileAndUbiquitousComputing.P2Photos.DataObjects.ResponseData;
import MobileAndUbiquitousComputing.P2Photos.Exceptions.FailedLogoutException;

public class Logout {

    public static void LogoutUser(Activity activity) {
        String username = SessionManager.username;
        Log.i("MSG", "Logout: " + username);

        String url = "http://p2photo-production.herokuapp.com/logout/" + username;
        RequestData rData = new RequestData(activity, RequestData.RequestType.LOGOUT, url);
        try {
            ResponseData result = new QueryManager().execute(rData).get();
            int code = result.getServerCode();
            if (code == 200) {
                Log.i("STATUS", "The logout operation was successful");
                SessionID.deleteSessionID(activity);
            }
            else {
                Log.i("STATUS", "The login operation was unsuccessful. Unknown error.");
                throw new FailedLogoutException();
            }
        }
        catch (ExecutionException | InterruptedException ex) {
            throw new FailedLogoutException(ex.getMessage());
        }
    }
}
