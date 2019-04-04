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
    public final static CookieManager cookieManager = new CookieManager();

    private Login() {
        // Prevents this class from being instantiated. //
    }

    // TODO - Use SessionManager and ConnectionManager to form everything. //
    public static void login(Activity activity, String username, String password) throws FailedLoginException {
        Log.i("MSG", "Login: " + username);

        String url = ConnectionManager.P2PHOTO_HOST + ConnectionManager.LOGIN_OPERATION;
        try {
            JSONObject requestBody = new JSONObject();
            requestBody.put("username", username);
            requestBody.put("password", password);
            RequestData requestData = new PostRequestData(activity, RequestData.RequestType.LOGIN, url, requestBody);

            ResponseData result = new QueryManager().execute(requestData).get();
            int code = result.getServerCode();
            if (code == 200) { // TODO Our server already supports ResponseEntities as taught by you.
                Log.i("STATUS", "The login operation was successful");

                SessionManager.username = username;
                SessionManager.password = password;
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
        catch (JSONException | ExecutionException | InterruptedException ex) {
            throw new FailedLoginException(ex.getMessage());
        }
    }
}
