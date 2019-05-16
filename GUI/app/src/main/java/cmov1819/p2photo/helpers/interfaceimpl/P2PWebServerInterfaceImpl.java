package cmov1819.p2photo.helpers.interfaceimpl;

import android.app.Activity;
import android.content.Intent;

import java.net.HttpURLConnection;
import java.security.KeyException;
import java.security.PublicKey;

import cmov1819.p2photo.LoginActivity;
import cmov1819.p2photo.R;
import cmov1819.p2photo.dataobjects.RequestData;
import cmov1819.p2photo.dataobjects.ResponseData;
import cmov1819.p2photo.exceptions.FailedOperationException;
import cmov1819.p2photo.helpers.ConvertUtils;
import cmov1819.p2photo.helpers.mediators.P2PWebServerMediator;
import cmov1819.p2photo.msgtypes.ErrorResponse;
import cmov1819.p2photo.msgtypes.SuccessResponse;

import static cmov1819.p2photo.LoginActivity.WIFI_DIRECT_SV_RUNNING;
import static cmov1819.p2photo.dataobjects.RequestData.RequestType.GET_MEMBER_PUBLIC_KEY;
import static cmov1819.p2photo.helpers.managers.LogManager.logInfo;
import static cmov1819.p2photo.helpers.managers.LogManager.logWarning;
import static cmov1819.p2photo.helpers.managers.LogManager.toast;

public class P2PWebServerInterfaceImpl {
    private static final String P2P_WEB_SV_INTERFACE_TAG = "P2PWebServerInterfaceImpl";

    public static PublicKey getMemberPublicKey(Activity activity, String username) {
        logInfo(P2P_WEB_SV_INTERFACE_TAG, "Retrieving " + username + "public key from P2P WebServer...");
        String url =
                activity.getString(R.string.p2photo_host) + activity.getString(R.string.get_member_key) + "?username=" + username;
        RequestData request = new RequestData(activity, GET_MEMBER_PUBLIC_KEY, url);
        try {
            ResponseData result = new P2PWebServerMediator().execute(request).get();
            int code = result.getServerCode();
            if (code == HttpURLConnection.HTTP_OK) {
                logInfo(P2P_WEB_SV_INTERFACE_TAG, "Retrieved. Converting...");
                SuccessResponse payload = (SuccessResponse) result.getPayload();
                String base64PublicKey = (String) payload.getResult();
                logInfo(P2P_WEB_SV_INTERFACE_TAG, "Converted...");
                return ConvertUtils.base64StringToPublicKey(base64PublicKey);
            } else if (code == HttpURLConnection.HTTP_NO_CONTENT) {
                logWarning(P2P_WEB_SV_INTERFACE_TAG,"Member: " + username + "has registered public key!");
                throw new KeyException("Member: " + username + "has registered public key!");
            } else if (code == HttpURLConnection.HTTP_UNAUTHORIZED) {
                String msg = ((ErrorResponse)result.getPayload()).getReason();
                logWarning(P2P_WEB_SV_INTERFACE_TAG, msg);
                toast(activity, "Session timed out, please login again");
                Intent loginIntent = new Intent(activity, LoginActivity.class);
                loginIntent.putExtra(WIFI_DIRECT_SV_RUNNING,true);
                activity.startActivity(loginIntent);
            }
        } catch (Exception e) {
            throw new FailedOperationException(e.getMessage());
        }
        return null;
    }
}
