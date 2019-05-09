package cmov1819.p2photo.helpers.architectures.wirelessP2PArchitecture;

import android.app.Activity;
import android.content.Context;
import android.widget.Toast;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import cmov1819.p2photo.helpers.managers.LogManager;

import static cmov1819.p2photo.helpers.ConvertUtils.inputStreamToString;

public class CatalogMerge {
    private CatalogMerge() {
        // Does not allow this class to be instantiated. //
    }

    public static void mergeCatalogFiles(final Activity activity,
                                         final String catalogId,
                                         final JSONObject anotherCatalogFileContents) {

        // Retrieve catalog file contents as a JSON Object and compare them to the received catalog file
        try {
            // Load contents
            String fileName = String.format("catalog_%s.json", catalogId);
            InputStream inputStream = activity.openFileInput(fileName);
            String thisCatalogFileContentsString = inputStreamToString(inputStream);
            JSONObject thisCatalogFileContents = new JSONObject(thisCatalogFileContentsString);
            JSONObject mergedContents =  mergeCatalogFileContents(thisCatalogFileContents, anotherCatalogFileContents);
            if (mergedContents == null) {
                Toast.makeText(activity, "Couldn't update catalog file", Toast.LENGTH_SHORT).show();
                LogManager.logWarning(LogManager.NEW_CATALOG_SLICE_TAG, "Catalog merging resulted in null JSONObject");
            } else {
                FileOutputStream outputStream = activity.openFileOutput(fileName, Context.MODE_PRIVATE);
                outputStream.write(mergedContents.toString().getBytes("UTF-8"));
                outputStream.close();
            }
        } catch (IOException | JSONException exc) {
            LogManager.logError(LogManager.NEW_CATALOG_TAG, exc.getMessage());
            LogManager.toast(activity, "Couldn't read stored catalog slice");
        }
    }

    private static JSONObject mergeCatalogFileContents(JSONObject thisFile, JSONObject otherFile) throws JSONException {
        String thisId = thisFile.getString("catalogId");
        String otherId = otherFile.getString("catalogId");

        if (!thisId.equals(otherId)) {
            return null;
        }

        JSONObject thisMembersPhotosMap = thisFile.getJSONObject("membersPhotos");
        JSONObject anotherMembersPhotosMap = otherFile.getJSONObject("membersPhotos");

        JSONObject mergedMembersPhotoMap = mergeMaps(thisMembersPhotosMap, anotherMembersPhotosMap);

        JSONObject mergedCatalogFileContents = new JSONObject();
        mergedCatalogFileContents.put("catalogId", thisId);
        mergedCatalogFileContents.put("catalogTitle", thisFile.getString("catalogTitle"));
        mergedCatalogFileContents.put("membersPhotos", mergedMembersPhotoMap);

        return mergedCatalogFileContents;
    }

    private static JSONObject mergeMaps(JSONObject thisMap, JSONObject receivedMap) {
        JSONObject mergedMap = new JSONObject();
        Iterator<String> receivedMembers = receivedMap.keys();
        while (receivedMembers.hasNext()) {
            String currentMember = receivedMembers.next();
            try {
                if (thisMap.has(currentMember)) {
                    List<String> thisCurrentMemberPhotos = jsonArrayToArrayList(receivedMap.getJSONArray(currentMember));
                    List<String> receivedCurrentMemberPhotos = jsonArrayToArrayList(thisMap.getJSONArray(currentMember));
                    thisCurrentMemberPhotos.addAll(receivedCurrentMemberPhotos);
                    List<String> mergedCurrentMemberPhotos = new ArrayList<>(new HashSet<>(thisCurrentMemberPhotos));
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

    private static List<String> jsonArrayToArrayList(JSONArray jsonArray) {
        //noinspection UnstableApiUsage
        return new Gson().fromJson(jsonArray.toString(), new TypeToken<List<String>>(){}.getType());
    }
}
