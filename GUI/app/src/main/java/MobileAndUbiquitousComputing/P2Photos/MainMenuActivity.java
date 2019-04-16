package MobileAndUbiquitousComputing.P2Photos;

import android.app.PendingIntent;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.fasterxml.jackson.databind.ObjectMapper;

import net.openid.appauth.AuthState;
import net.openid.appauth.AuthorizationException;
import net.openid.appauth.AuthorizationRequest;
import net.openid.appauth.AuthorizationResponse;
import net.openid.appauth.AuthorizationService;
import net.openid.appauth.AuthorizationServiceConfiguration;
import net.openid.appauth.TokenResponse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;

import org.json.JSONObject;

import MobileAndUbiquitousComputing.P2Photos.dataobjects.RequestData;
import MobileAndUbiquitousComputing.P2Photos.dataobjects.ResponseData;
import MobileAndUbiquitousComputing.P2Photos.exceptions.FailedOperationException;
import MobileAndUbiquitousComputing.P2Photos.helpers.AppContext;
import MobileAndUbiquitousComputing.P2Photos.helpers.AuthorizationCodes;
import MobileAndUbiquitousComputing.P2Photos.helpers.QueryManager;
import MobileAndUbiquitousComputing.P2Photos.helpers.SessionManager;

import static MobileAndUbiquitousComputing.P2Photos.helpers.AppContext.APP_LOG_TAG;
import static MobileAndUbiquitousComputing.P2Photos.helpers.SessionManager.AUTH_ENDPOINT;
import static MobileAndUbiquitousComputing.P2Photos.helpers.SessionManager.TOKEN_ENDPOINT;
import static MobileAndUbiquitousComputing.P2Photos.helpers.SessionManager.persistAuthState;
import static MobileAndUbiquitousComputing.P2Photos.helpers.SessionManager.restoreAuthState;

public class MainMenuActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);
    }

    @Override
    public void onBackPressed() {

    }

    public static class AuthorizeListener implements Button.OnClickListener {
        @Override
        public void onClick(View view) {
            // Declare the authorization and token endpoints of the OAuth server we wish to authorize with
            AuthorizationServiceConfiguration serviceConfiguration =
                    new AuthorizationServiceConfiguration(Uri.parse(AUTH_ENDPOINT), Uri.parse(TOKEN_ENDPOINT), null);

            // Describes actual authorization request, including our OAuth APP clientId and the scopes we are requesting
            String clientId = "327056365677-stsv6tntebv1f2jj8agkcr84vrbs3llk.apps.googleusercontent.com";
            Uri redirectUri = Uri.parse("https://127.0.0.1");                   // loopback address
            AuthorizationRequest.Builder builder = new AuthorizationRequest.Builder(
                    serviceConfiguration,
                    clientId,
                    AuthorizationCodes.RESPONSE_TYPE_CODE,
                    redirectUri
            );
            builder.setScopes("profile");                                       // Declare scope here. drive.files, etc.
            AuthorizationRequest request = builder.build();

            // Ideally, there is one instance of AuthorizationService per Activity
            AuthorizationService authorizationService = new AuthorizationService(view.getContext());

            // Create the PendingIntent to handle the authorization response
            String action = "MobileAndUbiquitousComputing.P2Photos.HANDLE_AUTHORIZATION_RESPONSE";
            Intent postAuthorizationIntent = new Intent(action);
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    view.getContext(), request.hashCode(), postAuthorizationIntent, 0
            );
            // Perform authorization request
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
    }

    @Override
    protected void onNewIntent(Intent intent) {
        checkIntent(intent);
    }

    private void checkIntent(@Nullable Intent intent) {
        if (intent != null) {
            String action = intent.getAction();
            assert action != null;
            switch (action) {
                case "MobileAndUbiquitousComputing.P2Photos.HANDLE_AUTHORIZATION_RESPONSE":
                    if (!intent.hasExtra(AppContext.USED_INTENT)) {
                        handleAuthorizationResponse(intent);
                        intent.putExtra(AppContext.USED_INTENT, true);
                    }
                    break;
                default:
                    break;
            }
        }
    }

    /*
     * AppAuth provides the AuthorizationResponse to this activity, via the provided RedirectUriReceiverActivity.
     * From it we can ultimately obtain a TokenResponse which we can use to make calls to the API;
     */
    private void handleAuthorizationResponse(@NonNull Intent intent) {
        // Obtain the AuthorizationResponse from the Intent
        AuthorizationResponse response = AuthorizationResponse.fromIntent(intent);
        AuthorizationException error = AuthorizationException.fromIntent(intent);
        // The AuthState object created here is a convenient way to store details from the authorization session.
        // We can update it with the results of new OAuth responses
        // We can also persist it to store the authorization session between app starts.
        final AuthState authState = new AuthState(response, error);

        // Exchange authorization code for the refresh and access tokens, and update the AuthState instance
        if (response != null) {
            Log.i(APP_LOG_TAG, "Handled Authorization Response " + authState.jsonSerialize().toString());
            AuthorizationService service = new AuthorizationService(this);
            service.performTokenRequest(response.createTokenExchangeRequest(), new AuthorizationService.TokenResponseCallback() {
                @Override
                public void onTokenRequestCompleted(@Nullable TokenResponse token, @Nullable AuthorizationException exc) {
                    if (exc != null) {
                        Log.w(APP_LOG_TAG, "Token Exchange failed", exc);
                    } else {
                        if (token != null) {
                            authState.update(token, exc);
                            persistAuthState(authState, MainMenuActivity.this);
                            enablePostAuthorizationFlows();
                            Log.i(APP_LOG_TAG, "Token Response [ Access Token: " + token.accessToken + ", ID Token: " + token.idToken + " ]");
                        }
                    }
                }
            });
        }
    }

    private void enablePostAuthorizationFlows() {
        // TODO
        /*
        mAuthState = restoreAuthState(MainMenuActivity.this);
        if (mAuthState != null && mAuthState.isAuthorized()) {
            if (mMakeApiCall.getVisibility() == View.GONE) {
                mMakeApiCall.setVisibility(View.VISIBLE);
                mMakeApiCall.setOnClickListener(new MakeApiCallListener(this, mAuthState, new AuthorizationService(this)));
            }
            if (mSignOut.getVisibility() == View.GONE) {
                mSignOut.setVisibility(View.VISIBLE);
                mSignOut.setOnClickListener(new SignOutListener(this));
            }
        } else {
            mMakeApiCall.setVisibility(View.GONE);
            mSignOut.setVisibility(View.GONE);
        }
        */
    }

    @Override
    protected void onStart() {
        super.onStart();
        checkIntent(getIntent());
    }

    public void viewAlbumClicked(View view) {
        Intent intent = new Intent(this, ShowAlbumActivity.class);
        startActivity(intent);
    }

    public void createAlbumClicked(View view) {
        Intent intent = new Intent(this, NewAlbumActivity.class);
        startActivity(intent);
    }

    public void FindUserClicked(View view) {
        Intent intent = new Intent(this, SearchUserActivity.class);
        startActivity(intent);
    }

    public void AddPhotosClicked(View view) {
        Intent intent = new Intent(this, AddPhotosActivity.class);
        startActivity(intent);
    }

    public void AddUsersClicked(View view) {
        Intent intent = new Intent(this, NewAlbumMemberActivity.class);
        startActivity(intent);
    }

    public void ListAlbumsClicked(View view) {
        ArrayList<String> items = new ArrayList<>(Arrays.asList("239287741","401094244","519782246"));
        Intent intent = new Intent(this, ShowUserAlbumsActivity.class);
        intent.putStringArrayListExtra("catalogs", items);
        startActivity(intent);
    }

    public void LogoutClicked(View view) {
        logout();
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
    }

    public void logout() {
        String username = SessionManager.getUsername(this);
        Log.i("MSG", "Logout: " + username);

        String url =
                getString(R.string.p2photo_host) + getString(R.string.logout_operation) + username;
        RequestData rData = new RequestData(this, RequestData.RequestType.LOGOUT, url);
        try {
            ResponseData result = new QueryManager().execute(rData).get();
            int code = result.getServerCode();
            if (code == 200) {
                Log.i("STATUS", "The logout operation was successful");
                SessionManager.deleteSessionID(this);
                SessionManager.deleteUserName(this);
            } else {
                Log.i("STATUS", "The login operation was unsuccessful. Unknown error.");
                throw new FailedOperationException();
            }
        } catch (ExecutionException | InterruptedException ex) {
            throw new FailedOperationException(ex.getMessage());
        }
    }
}
