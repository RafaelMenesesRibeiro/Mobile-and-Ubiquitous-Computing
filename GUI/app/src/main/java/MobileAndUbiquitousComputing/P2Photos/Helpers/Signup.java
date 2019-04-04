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
import MobileAndUbiquitousComputing.P2Photos.Exceptions.FailedSignupException;
import MobileAndUbiquitousComputing.P2Photos.Exceptions.WrongCredentialsException;

public class Signup {
    public static void SignupUser(Activity activity, String username, String password) throws FailedLoginException{
        Log.i("MSG", "Signup: " + username);
        String url = ConnectionManager.P2PHOTO_HOST + ConnectionManager.SIGNUP_OPERATION;
        try {
            JSONObject requestBody = new JSONObject();
            requestBody.put("username", username);
            requestBody.put("password", password);
            RequestData requestData = new PostRequestData(activity, RequestData.RequestType.SIGNUP, url, requestBody);

            ResponseData result = new QueryManager().execute(requestData).get();
            int code = result.getServerCode();
            // TODO - Response codes are now in a ResponseEntity. //
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
        catch (JSONException | ExecutionException | InterruptedException ex) {
            throw new FailedSignupException(ex.getMessage());
        }

        try {
            Login.login(activity, username, password);
        }
        catch (WrongCredentialsException wcex) {
            // Do nothing.
            // SHOULD NEVER BE HERE. AS THE CREDENTIALS WERE USED WITHOUT CHANGE FOR SIGNING UP.
        }
    }
}
