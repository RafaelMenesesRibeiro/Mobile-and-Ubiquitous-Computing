package cmov1819.p2photo.helpers.driveasynctasks;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static cmov1819.p2photo.helpers.managers.GoogleDriveManager.*;

public class CreateFolder extends AsyncTask<String, Void, JSONObject> {
    private String folderName;
    private String rootFolderId;
    private String accessToken;
    private String idToken;

    public CreateFolder(String folderName, String accessToken, String idToken) {
        this.folderName = folderName;
        this.rootFolderId = "root";
        this.accessToken = accessToken;
        this.idToken = idToken;
    }

    public CreateFolder(String folderName, String rootFolderId, String accessToken, String idToken) {
        this.folderName = folderName;
        this.rootFolderId = rootFolderId;
        this.accessToken = accessToken;
        this.idToken = idToken;
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

    private JSONObject newDirectory(String folderName) throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("title", folderName);
        jsonObject.put("mimeType", TYPE_GOOGLE_DRIVE_FOLDER);

        if (!rootFolderId.equals("root")) {
            jsonObject.put("parents", String.format("[{ \"id\" : \"%s\" }]", rootFolderId));
        }

        return jsonObject;
    }
}
