package MobileAndUbiquitousComputing.P2Photos.Helpers;

import android.app.Activity;
import android.util.Log;

import java.util.concurrent.ExecutionException;

import MobileAndUbiquitousComputing.P2Photos.DataObjects.RequestData;
import MobileAndUbiquitousComputing.P2Photos.DataObjects.ResponseData;
import MobileAndUbiquitousComputing.P2Photos.Exceptions.FailedLogoutException;

public class Logout {
    private Logout() {
        // Prevents this class from being instantiated. //
    }

    public static void logout(Activity activity) {
        String username = SessionManager.username;
        Log.i("MSG", "Logout: " + username);

        String url = ConnectionManager.P2PHOTO_HOST + ConnectionManager.LOGOUT_OPERATION + username;
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
