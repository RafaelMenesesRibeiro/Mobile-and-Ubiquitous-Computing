package cmov1819.p2photo.helpers.interfaceimpl;

import android.app.Activity;
import android.content.Intent;

import java.net.HttpURLConnection;
import java.security.PublicKey;
import java.util.concurrent.ExecutionException;

import cmov1819.p2photo.LoginActivity;
import cmov1819.p2photo.R;
import cmov1819.p2photo.dataobjects.RequestData;
import cmov1819.p2photo.dataobjects.ResponseData;
import cmov1819.p2photo.exceptions.FailedOperationException;
import cmov1819.p2photo.exceptions.RSAException;
import cmov1819.p2photo.helpers.ConvertUtils;
import cmov1819.p2photo.helpers.managers.SessionManager;
import cmov1819.p2photo.helpers.mediators.P2PWebServerMediator;
import cmov1819.p2photo.msgtypes.ErrorResponse;
import cmov1819.p2photo.msgtypes.SuccessResponse;

import static cmov1819.p2photo.LoginActivity.WIFI_DIRECT_SV_RUNNING;
import static cmov1819.p2photo.R.string.assert_membership;
import static cmov1819.p2photo.R.string.p2photo_host;
import static cmov1819.p2photo.dataobjects.RequestData.RequestType.GET_MEMBER_PUBLIC_KEY;
import static cmov1819.p2photo.helpers.managers.LogManager.logInfo;
import static cmov1819.p2photo.helpers.managers.LogManager.logWarning;
import static cmov1819.p2photo.helpers.managers.LogManager.toast;

public class P2PWebServerInterfaceImpl {
    private static final String P2P_WEB_SV_INTERFACE_TAG = "P2PWebServerInterfaceImpl";

    public static boolean assertMembership(Activity activity, String username, String catalogId) {
        logInfo(P2P_WEB_SV_INTERFACE_TAG, "Querying P2PWebServer regarding user membership");
        String baseUrl = "%s%s?calleeUsername=%s&toGetUsername=%s&catalogID=%s";
        String url = String.format(
                baseUrl, activity.getString(p2photo_host), activity.getString(assert_membership), SessionManager.getUsername(activity), username, catalogId);

        RequestData request = new RequestData(activity, GET_MEMBER_PUBLIC_KEY, url);
        try {
            ResponseData result = new P2PWebServerMediator().execute(request).get();
            int code = result.getServerCode();
            if (code == HttpURLConnection.HTTP_OK) {
                logInfo(P2P_WEB_SV_INTERFACE_TAG, "Retrieved membership result...");
                SuccessResponse payload = (SuccessResponse) result.getPayload();
                return (Integer) payload.getResult() == 0;
            } else if (code == HttpURLConnection.HTTP_UNAUTHORIZED) {
                logWarning(P2P_WEB_SV_INTERFACE_TAG, ((ErrorResponse)result.getPayload()).getReason());
                toast(activity, "Session timed out, please login again");
                Intent loginIntent = new Intent(activity, LoginActivity.class);
                loginIntent.putExtra(WIFI_DIRECT_SV_RUNNING,true);
                activity.startActivity(loginIntent);
            } else {
                logWarning(P2P_WEB_SV_INTERFACE_TAG, ((ErrorResponse)result.getPayload()).getReason());
            }
        } catch (ExecutionException | InterruptedException exc) {
            throw new FailedOperationException(exc.getMessage());
        }
        return false;
    }

    public static PublicKey getMemberPublicKey(Activity activity, String username) {
        logInfo(P2P_WEB_SV_INTERFACE_TAG, "Retrieving " + username + "public key from P2P WebServer...");
        String url =
                activity.getString(p2photo_host) + activity.getString(R.string.get_member_key) + "?calleeUsername=" + SessionManager.getUsername(activity) + "&toGetUsername=" + username;
        RequestData request = new RequestData(activity, GET_MEMBER_PUBLIC_KEY, url);
        try {
            ResponseData result = new P2PWebServerMediator().execute(request).get();
            int code = result.getServerCode();
            if (code == HttpURLConnection.HTTP_OK) {
                logInfo(P2P_WEB_SV_INTERFACE_TAG, "Retrieved. Converting...");
                SuccessResponse payload = (SuccessResponse) result.getPayload();
                String base64PublicKey = (String) payload.getResult();
                return ConvertUtils.base64StringToPublicKey(base64PublicKey);
            } else if (code == HttpURLConnection.HTTP_NO_CONTENT) {
                logWarning(P2P_WEB_SV_INTERFACE_TAG,"Member: " + username + "has no registered public key!");
            } else if (code == HttpURLConnection.HTTP_UNAUTHORIZED) {
                logWarning(P2P_WEB_SV_INTERFACE_TAG, ((ErrorResponse)result.getPayload()).getReason());
                toast(activity, "Session timed out, please login again");
                Intent loginIntent = new Intent(activity, LoginActivity.class);
                loginIntent.putExtra(WIFI_DIRECT_SV_RUNNING,true);
                activity.startActivity(loginIntent);
            }
        } catch (ExecutionException | InterruptedException | RSAException exc) {
            throw new FailedOperationException(exc.getMessage());
        }
        return null;
    }
}
