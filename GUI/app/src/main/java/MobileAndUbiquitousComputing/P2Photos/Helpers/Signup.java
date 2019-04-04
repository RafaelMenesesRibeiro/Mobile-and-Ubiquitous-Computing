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
import MobileAndUbiquitousComputing.P2Photos.Exceptions.UsernameExistsException;
import MobileAndUbiquitousComputing.P2Photos.Exceptions.WrongCredentialsException;
import MobileAndUbiquitousComputing.P2Photos.R;

public class Signup {
    public static void SignupUser(Activity activity, String username, String password)
            throws FailedSignupException, FailedLoginException, UsernameExistsException {
        Log.i("MSG", "Signup: " + username);
        String url = activity.getString(R.string.p2photo_host) + activity.getString(R.string.signup_operation);
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
                throw new UsernameExistsException();
            }
            else {
                Log.i("STATUS", "The sign up operation was unsuccessful. Unknown error.");
                throw new FailedSignupException();
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
