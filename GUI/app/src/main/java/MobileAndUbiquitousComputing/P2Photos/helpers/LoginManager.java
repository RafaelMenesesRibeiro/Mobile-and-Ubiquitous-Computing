package MobileAndUbiquitousComputing.P2Photos.helpers;

import android.app.Activity;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.util.concurrent.ExecutionException;

import MobileAndUbiquitousComputing.P2Photos.R;
import MobileAndUbiquitousComputing.P2Photos.dataobjects.PostRequestData;
import MobileAndUbiquitousComputing.P2Photos.dataobjects.RequestData;
import MobileAndUbiquitousComputing.P2Photos.dataobjects.ResponseData;
import MobileAndUbiquitousComputing.P2Photos.exceptions.FailedLoginException;
import MobileAndUbiquitousComputing.P2Photos.exceptions.WrongCredentialsException;

public class LoginManager {

    public static void login(Activity activity, String username, String password) throws FailedLoginException {
        Log.i("MSG", "LoginManager: " + username);

        String url =
                activity.getString(R.string.p2photo_host) + activity.getString(R.string.login_operation);
        try {
            JSONObject requestBody = new JSONObject();
            requestBody.put("username", username);
            requestBody.put("password", password);
            RequestData requestData = new PostRequestData(activity, RequestData.RequestType.LOGIN
                    , url, requestBody);

            ResponseData result = new QueryManager().execute(requestData).get();
            int code = result.getServerCode();
            if (code == HttpURLConnection.HTTP_OK) {
                Log.i("STATUS", "The login operation was successful");

                SessionManager.updateUserName(activity, username);
                // The SessionID cookie is stored in getJSONStringFromHttpResponse //
            } else if (code == HttpURLConnection.HTTP_UNAUTHORIZED) {
                Log.i("STATUS", "The login operation was unsuccessful. The username or password " +
                        "are incorrect.");
                throw new WrongCredentialsException();
            } else {
                Log.i("STATUS",
                        "The login operation was unsuccessful. Server response code: " + code);
                throw new FailedLoginException();
            }
        } catch (JSONException | ExecutionException | InterruptedException ex) {
            throw new FailedLoginException(ex.getMessage());
        }
    }
}
