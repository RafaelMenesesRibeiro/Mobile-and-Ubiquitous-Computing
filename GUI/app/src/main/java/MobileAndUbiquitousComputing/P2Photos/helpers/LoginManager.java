package MobileAndUbiquitousComputing.P2Photos.helpers;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.View;

import net.openid.appauth.AuthorizationRequest;
import net.openid.appauth.AuthorizationService;
import net.openid.appauth.AuthorizationServiceConfiguration;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;

import MobileAndUbiquitousComputing.P2Photos.R;
import MobileAndUbiquitousComputing.P2Photos.dataobjects.PostRequestData;
import MobileAndUbiquitousComputing.P2Photos.dataobjects.RequestData;
import MobileAndUbiquitousComputing.P2Photos.dataobjects.ResponseData;
import MobileAndUbiquitousComputing.P2Photos.exceptions.FailedLoginException;
import MobileAndUbiquitousComputing.P2Photos.exceptions.WrongCredentialsException;

import static MobileAndUbiquitousComputing.P2Photos.helpers.SessionManager.AUTH_ENDPOINT;
import static MobileAndUbiquitousComputing.P2Photos.helpers.SessionManager.TOKEN_ENDPOINT;

public class LoginManager {
    /*
    * APP_ID is the application ID the client is going to request an OAuth Token for;
    * REDIRECT_URI is the loopback address.
    */
    private static final String LOGIN_MGR_TAG = "LOGIN MGR";
    private static final String APP_ID = "327056365677-stsv6tntebv1f2jj8agkcr84vrbs3llk.apps.googleusercontent.com";
    private static final Uri REDIRECT_URI = Uri.parse("https://127.0.0.1");

    public static void tryLogin(Activity activity, String username, String password) throws FailedLoginException {
        Log.i(LOGIN_MGR_TAG, "Starting tryLogin operation for username: " + username + "...");

        try {
            String url = activity.getString(R.string.p2photo_host) + activity.getString(R.string.login_operation);

            JSONObject requestBody = new JSONObject();
            requestBody.put("username", username);
            requestBody.put("password", password);

            RequestData requestData = new PostRequestData(activity, RequestData.RequestType.LOGIN, url, requestBody);

            ResponseData result = new QueryManager().execute(requestData).get();

            int code = result.getServerCode();
            if (code == HttpURLConnection.HTTP_OK) {
                Log.i(LOGIN_MGR_TAG, "The tryLogin operation succeded");
                SessionManager.updateUserName(activity, username);
            } else if (code == HttpURLConnection.HTTP_UNAUTHORIZED) {
                Log.i(LOGIN_MGR_TAG, "The tryLogin operation failed. The username or password are incorrect.");
                throw new WrongCredentialsException();
            } else {
                Log.i(LOGIN_MGR_TAG,"The tryLogin operation failed. Server error with response code: " + code);
                throw new FailedLoginException();
            }

        } catch (JSONException | ExecutionException | InterruptedException ex) {
            throw new FailedLoginException(ex.getMessage());
        }
    }

    public static void validateGooglelAPIAuthorization(View view) {
        if (existsPersistedAuthState()) {
            tryRefreshAuthStateTokens();
        } else {
            tryGetAuthState(view);
        }
    }

    private static boolean existsPersistedAuthState() {
        return false;
    }

    private static void tryRefreshAuthStateTokens() {
        // TODO
    }

    private static void tryGetAuthState(View view) {
        // Generate an authorization request with scopes the user should authorize this app to manage
        AuthorizationRequest request = newAuthorizationRequest();
        // Ideally, there is one instance of AuthorizationService per Activity
        AuthorizationService authorizationService = new AuthorizationService(view.getContext());
        // Create the PendingIntent to handle the authorization request response
        String action = "MobileAndUbiquitousComputing.P2Photos.HANDLE_AUTHORIZATION_RESPONSE";
        Intent postAuthorizationIntent = new Intent(action);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                view.getContext(), request.hashCode(), postAuthorizationIntent, 0
        );
        authorizationService.performAuthorizationRequest(request, pendingIntent);

            /*
                To add handling for the authorization response. We have to add hooks into the app in order to receive
            the authorization response from the browser.

                First we register RedirectUriReceiverActivity intent-filters in AndroidManifest.xml, inside the
            <application> block. This registers the app to receive the OAuth2 authorization response intent from the
            system browser on our behalf.

                Then, add a new intent-filter to your <activity android:name=".MainActivity"> so AppAuth can pass the
            authorization response to your main activity.

                After updating the Android Manifest file we need to create methods to handle the received intents:
                #onNewIntent
                #checkIntent
                #onStart
            */
    }

    /**********************************************************
     * HELPER METHODS
     ***********************************************************/

    /* Describes an authorization request, including the application clientId for the OAuth and the respective scopes */
    private static AuthorizationRequest newAuthorizationRequest() {
        AuthorizationRequest.Builder builder = new AuthorizationRequest.Builder(
                new AuthorizationServiceConfiguration(Uri.parse(AUTH_ENDPOINT), Uri.parse(TOKEN_ENDPOINT), null),
                APP_ID,
                AuthorizationCodes.RESPONSE_TYPE_CODE,
                REDIRECT_URI
        );
        builder.setScopes(new ArrayList<>(Arrays.asList(
            "email", "profile", "openid",
            "https://www.googleapis.com/auth/drive.photos.readonly",
            "https://www.googleapis.com/auth/drive.metadata.readonly",
            "https://www.googleapis.com/auth/drive.readonly",
            "https://www.googleapis.com/auth/drive.file",
            "https://www.googleapis.com/auth/drive.appdata"
        )));
        return builder.build();
    }
}
