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

import static cmov1819.p2photo.helpers.managers.GoogleDriveManager.AUTHORIZATION_HEADER;
import static cmov1819.p2photo.helpers.managers.GoogleDriveManager.FILE_UPLOAD_ENDPOINT;
import static cmov1819.p2photo.helpers.managers.GoogleDriveManager.GOOGLE_DRIVE_TAG;
import static cmov1819.p2photo.helpers.managers.GoogleDriveManager.JSON_TYPE;
import static cmov1819.p2photo.helpers.managers.GoogleDriveManager.TYPE_TXT;
import static cmov1819.p2photo.helpers.managers.GoogleDriveManager.processErrorCodes;

public class CreateFile extends AsyncTask<String, Void, JSONObject> {
    private final String fileName;
    private final String rootFolderId;
    private final String accessToken;
    private final String idToken;

    public CreateFile(String fileName, String rootFolderId, String accessToken, String idToken) {
        this.fileName = fileName;
        this.rootFolderId = rootFolderId;
        this.accessToken = accessToken;
        this.idToken = idToken;
    }

    public static String processResponse(Context context, JSONObject response) throws JSONException {
        String fileId = null;
        if (response == null) {
            Log.e(GOOGLE_DRIVE_TAG, "createFile method resulted in a null response from google API.");
        } else if (response.has("error")) {
            Log.e(GOOGLE_DRIVE_TAG,"createFile response had error: " + response.getString("message"));
            processErrorCodes(context, response);
        } else {
            Log.i(GOOGLE_DRIVE_TAG, "Created file with success");
            return response.getString("id");
        }
        return fileId;
    }

    @Override
    protected JSONObject doInBackground(String... strings) {
        try {
            OkHttpClient okHttpClient = new OkHttpClient();
            RequestBody body = RequestBody.create(JSON_TYPE, newFile().toString());
            Request request = new Request.Builder()
                    .url(FILE_UPLOAD_ENDPOINT)
                    .post(body)
                    .addHeader(AUTHORIZATION_HEADER, "Bearer " + accessToken)
                    .build();
            Response response = okHttpClient.newCall(request).execute();;
            String jsonBody = response.body().string();
            Log.i(GOOGLE_DRIVE_TAG, "createFile response: " + jsonBody);
            return new JSONObject(jsonBody);
        } catch (Exception exception) {
            Log.e(GOOGLE_DRIVE_TAG, exception.getMessage());
            return null;
        }
    }

    private JSONObject newFile() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("title", fileName);
        jsonObject.put("mimeType", TYPE_TXT);

        if (!rootFolderId.equals("root")) {
            jsonObject.put("parents", String.format("[{ \"id\" : \"%s\" }]", rootFolderId));
        }

        return jsonObject;
    }
}
