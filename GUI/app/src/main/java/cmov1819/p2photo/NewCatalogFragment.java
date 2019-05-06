package cmov1819.p2photo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;

import cmov1819.p2photo.dataobjects.PostRequestData;
import cmov1819.p2photo.dataobjects.PutRequestData;
import cmov1819.p2photo.dataobjects.RequestData;
import cmov1819.p2photo.dataobjects.ResponseData;
import cmov1819.p2photo.exceptions.FailedOperationException;
import cmov1819.p2photo.helpers.UserSlice;
import cmov1819.p2photo.helpers.managers.ArchitectureManager;
import cmov1819.p2photo.helpers.managers.LogManager;
import cmov1819.p2photo.helpers.managers.QueryManager;
import cmov1819.p2photo.helpers.managers.SessionManager;
import cmov1819.p2photo.msgtypes.ErrorResponse;
import cmov1819.p2photo.msgtypes.SuccessResponse;

import static android.widget.Toast.LENGTH_LONG;
import static cmov1819.p2photo.helpers.ConvertUtils.inputStreamToString;
import static cmov1819.p2photo.helpers.managers.SessionManager.getUsername;

public class NewCatalogFragment extends Fragment {
    private Activity activity;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        activity = getActivity();
        final View view = inflater.inflate(R.layout.fragment_new_catalog, container, false);
        populate(view);
        return view;
    }

    private void populate(final View view) {
        Button doneButton = view.findViewById(R.id.done);
        doneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                newCatalogClicked(view);
            }
        });
        EditText editText = view.findViewById(R.id.nameInputBox);
        MainMenuActivity.bingEditTextWithButton(editText, doneButton);
    }

    public void newCatalogClicked(View view) {
        EditText titleInput = view.findViewById(R.id.nameInputBox);
        String catalogTitle = titleInput.getText().toString();

        if (catalogTitle.equals("")) {
            Toast toast = Toast.makeText(this.getContext(), "Enter a name for the album", Toast.LENGTH_LONG);
            toast.show();
            return;
        }

        try {
            String catalogId = newCatalog(catalogTitle);
            LogManager.logNewCatalog(catalogId, catalogTitle);
            MainMenuActivity mainMenuActivity = (MainMenuActivity) activity;
            mainMenuActivity.goToCatalog(catalogId, catalogTitle);
        }
        catch (FailedOperationException foex) {
            Toast.makeText(this.getContext(), "The create catalog operation failed. Try again later", Toast.LENGTH_LONG).show();
        }
        catch (NullPointerException | ClassCastException ex) {
            Toast.makeText(activity, "Could not present new catalog", Toast.LENGTH_LONG).show();
        }
    }

    private String newCatalog(String catalogTitle) {
        String url = getString(R.string.p2photo_host) + getString(R.string.new_catalog);

        try {
            JSONObject requestBody = new JSONObject();
            requestBody.put("catalogTitle", catalogTitle);
            requestBody.put("calleeUsername", getUsername(activity));
            RequestData requestData = new PostRequestData(activity, RequestData.RequestType.NEW_CATALOG, url, requestBody);
            ResponseData result = new QueryManager().execute(requestData).get();

            String catalogID;
            int code = result.getServerCode();
            if (code == HttpURLConnection.HTTP_OK) {
                SuccessResponse payload = (SuccessResponse) result.getPayload();
                catalogID = (String) payload.getResult();
            }
            else {
                ErrorResponse errorResponse = (ErrorResponse) result.getPayload();
                String msg = "The new catalog operation was unsuccessful. Server response code: " + code + ".\n" + result.getPayload().getMessage() + "\n" + errorResponse.getReason();
                LogManager.logError(LogManager.NEW_CATALOG_TAG, msg);
                throw new FailedOperationException();
            }

            ArchitectureManager.systemArchitecture.newCatalogSlice(activity, catalogID, catalogTitle);
            return catalogID;
        }
        catch (JSONException ex) {
            throw new FailedOperationException(ex.getMessage());
        }
        catch (ExecutionException | InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new FailedOperationException(ex.getMessage());
        }
    }

    public static void newCatalogSliceCloudArch(final Context context,
                                                final String catalogId,
                                                final String parentFolderGoogleId,
                                                final String catalogFileGoogleId,
                                                final String webContentLink) {
        try {

            JSONObject requestBody = new JSONObject();

            requestBody.put("parentFolderGoogleId", parentFolderGoogleId);
            requestBody.put("catalogFileGoogleId", catalogFileGoogleId);
            requestBody.put("webContentLink", webContentLink);
            requestBody.put("calleeUsername", getUsername((Activity)context));

            String url =
                    context.getString(R.string.p2photo_host) + context.getString(R.string.new_catalog_slice) + catalogId;

            RequestData requestData = new PutRequestData(
                    (Activity)context, RequestData.RequestType.NEW_CATALOG_SLICE, url, requestBody
            );

            ResponseData result = new QueryManager().execute(requestData).get();

            int code = result.getServerCode();

            if (code != HttpURLConnection.HTTP_OK) {
                String reason = ((ErrorResponse) result.getPayload()).getReason();
                if (code == HttpURLConnection.HTTP_UNAUTHORIZED) {
                    LogManager.logError(LogManager.NEW_CATALOG_TAG, reason);
                    Toast.makeText(context, "Session timed out, please login again", Toast.LENGTH_SHORT).show();
                    context.startActivity(new Intent(context, LoginActivity.class));
                }
                else {
                    LogManager.logError(LogManager.NEW_CATALOG_TAG, reason);
                    Toast.makeText(context, "Something went wrong", LENGTH_LONG).show();
                }
            }

        }
        catch (JSONException ex) {
            String msg = "JSONException: " + ex.getMessage();
            LogManager.logError(LogManager.NEW_CATALOG_TAG, msg);
            throw new FailedOperationException(ex.getMessage());
        }
        catch (ExecutionException | InterruptedException ex) {
            Thread.currentThread().interrupt();
            String msg = "New Catalog unsuccessful. " + ex.getMessage();
            LogManager.logError(LogManager.NEW_CATALOG_TAG, msg);
            throw new FailedOperationException(ex.getMessage());
        }
    }

    public static void newCatalogSliceWifiDirectArch(final Activity activity,
                                                     final String catalogId,
                                                     final String catalogTitle) {

        final String username = SessionManager.getUsername(activity);

        // Make catalog folder if it doesn't exist in private storage, otherwise retrieve it
        java.io.File catalogFolder = activity.getDir(catalogId, Context.MODE_PRIVATE);
        // Create catalog.json file
        try {
            // Create file content representation
            Hashtable<String, List<String>> membersPhotosMap = new Hashtable<>();
            membersPhotosMap.put(username, new ArrayList<String>());
            JSONObject catalogFile = new JSONObject();
            catalogFile.put("catalogId", catalogId);
            catalogFile.put("catalogTitle", catalogTitle);
            catalogFile.put("membersPhotos", membersPhotosMap);
            // Write them to application storage space
            String filePath = catalogFolder.getAbsolutePath() + "/catalog.json";
            FileOutputStream outputStream = activity.openFileOutput(filePath, Context.MODE_PRIVATE);
            outputStream.write(catalogFile.toString().getBytes("UTF-8"));
            outputStream.close();
        } catch (JSONException | IOException exc) {
            Toast.makeText(activity, "Failed to create catalog slice", Toast.LENGTH_SHORT).show();
            LogManager.logError(LogManager.NEW_CATALOG_TAG, exc.getMessage());
        }
    }

    public static void mergeCatalogSlicesWifiDirectArch(final Activity activity,
                                                        final String catalogId,
                                                        final JSONObject anotherCatalogFileContents) {

        // Get catalog folder path from application private storage
        String catalogFolderDir = activity.getDir(catalogId, Context.MODE_PRIVATE).getAbsolutePath();
        // Retrieve catalog file contents as a JSON Object and compare them to the received catalog file
        try {
            // Load contents
            InputStream inputStream = activity.openFileInput(catalogFolderDir + "/catalog.json");
            String thisCatalogFileContentsString = inputStreamToString(inputStream);
            JSONObject thisCatalogFileContents = new JSONObject(thisCatalogFileContentsString);
            JSONObject mergedContents =  mergeCatalogFileContents(thisCatalogFileContents, anotherCatalogFileContents);

        } catch (IOException | JSONException exc) {
            Toast.makeText(activity, "Couldn't read stored catalog slice", Toast.LENGTH_SHORT).show();
            LogManager.logError(LogManager.NEW_CATALOG_TAG, exc.getMessage());
        }
    }

    private static JSONObject mergeCatalogFileContents(JSONObject thisFile, JSONObject otherFile)
            throws JSONException {

        String thisId = thisFile.getString("catalogId");
        String otherId = otherFile.getString("catalogId");

        if (!thisId.equals(otherId)) {
            return null;
        }

        JSONObject thisMembersPhotosMap = thisFile.getJSONObject("membersPhotos");
        JSONObject anotherMembersPhotosMap = otherFile.getJSONObject("membersPhotos");

        JSONObject mergedMembersPhotoMap = processMembersMaps(thisMembersPhotosMap, anotherMembersPhotosMap);

        JSONObject mergedCatalogFileContents = new JSONObject();
        mergedCatalogFileContents.put("catalogId", thisId);
        mergedCatalogFileContents.put("catalogTitle", thisFile.getString("catalogTitle"));
        mergedCatalogFileContents.put("members", mergedMembersPhotoMap);

        return mergedCatalogFileContents;
    }

    private static JSONObject processMembersMaps(JSONObject thisMap, JSONObject receivedMap) {

        JSONObject mergedMap = new JSONObject();

        Iterator<String> receivedMembers = receivedMap.keys();

        while (receivedMembers.hasNext()) {
            String currentMember = receivedMembers.next();
            try {
                if (thisMap.has(currentMember)) {
                    List<String> thisCurrentMemberPhotos = jsonArrayToArrayList(receivedMap.getJSONArray(currentMember));
                    List<String> receivedCurrentMemberPhotos = jsonArrayToArrayList(thisMap.getJSONArray(currentMember));
                    thisCurrentMemberPhotos.addAll(receivedCurrentMemberPhotos);
                    List<String> mergedCurrentMemberPhotos =
                            new ArrayList<>(new HashSet<>(thisCurrentMemberPhotos));

                    mergedMap.put(currentMember, mergedCurrentMemberPhotos);
                }
                else {
                    mergedMap.put(currentMember, receivedMap.getJSONArray(currentMember));
                }
            } catch (JSONException jsone) {
                continue;
            }
        }
        return mergedMap;
    }

    public static List<String> jsonArrayToArrayList(JSONArray jsonArray) {
        return new Gson().fromJson(jsonArray.toString(), new TypeToken<List<String>>(){}.getType());
    }
}
