package cmov1819.p2photo.helpers.GoogleDriveAPIRequests;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import net.openid.appauth.AuthState;
import net.openid.appauth.AuthorizationService;

import org.json.JSONException;
import org.json.JSONObject;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static cmov1819.p2photo.helpers.GoogleDriveInteractor.*;
import static java.lang.Thread.sleep;

public class CreateFolder extends AsyncTask<String, Void, JSONObject> {
    private final String folderName;
    private final String rootFolder;
    private final String accessToken;
    private final String idToken;

    public CreateFolder(String folderName, String accessToken, String idToken) {
        this.folderName = folderName;
        this.rootFolder = "root";
        this.accessToken = accessToken;
        this.idToken = idToken;
    }

    public CreateFolder(String folderName, String rootFolder, String accessToken, String idToken) {
        this.folderName = folderName;
        this.rootFolder = rootFolder;
        this.accessToken = accessToken;
        this.idToken = idToken;
    }

    public static String processResponse(Context context, JSONObject response) throws JSONException {
        String folderId = null;
        if (response == null) {
            Log.e(GOOGLE_DRIVE_TAG, "createFolder method resulted in a null response from google API.");
        } else if (response.has("error")) {
            Log.e(GOOGLE_DRIVE_TAG,"createFolder response had error: " + response.getString("message"));
            processErrorCodes(context, response);
        } else {
            Log.i(GOOGLE_DRIVE_TAG, "Created folder with success");
            return response.getString("id");
        }
        return folderId;
    }

    @Override
    protected JSONObject doInBackground(String... strings) {
        try {
            OkHttpClient okHttpClient = new OkHttpClient();
            RequestBody body = RequestBody.create(JSON_TYPE, newDirectory(folderName).toString());
            Request request = new Request.Builder()
                    .url(FILE_UPLOAD_ENDPOINT)
                    .post(body)
                    .addHeader(AUTHORIZATION_HEADER, "Bearer " + accessToken)
                    .build();
            Response response = okHttpClient.newCall(request).execute();;
            String jsonBody = response.body().string();
            Log.i(GOOGLE_DRIVE_TAG, "createFolder response: " + jsonBody);
            return new JSONObject(jsonBody);
        } catch (Exception exception) {
            Log.e(GOOGLE_DRIVE_TAG, exception.getMessage());
            return null;
        }
    }

    private static JSONObject newDirectory(String folderName) throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("title", folderName);
        jsonObject.put("mimeType", TYPE_GOOGLE_DRIVE_FOLDER);
        return jsonObject;
    }
}
