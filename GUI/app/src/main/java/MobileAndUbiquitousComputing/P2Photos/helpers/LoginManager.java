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


}
