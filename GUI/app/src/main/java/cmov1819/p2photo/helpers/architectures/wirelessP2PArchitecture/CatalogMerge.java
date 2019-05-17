package cmov1819.p2photo.helpers.architectures.wirelessP2PArchitecture;

import android.app.Activity;
import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import cmov1819.p2photo.helpers.ConvertUtils;
import cmov1819.p2photo.helpers.managers.LogManager;

import static cmov1819.p2photo.helpers.ConvertUtils.inputStreamToString;
import static cmov1819.p2photo.helpers.managers.LogManager.logError;
import static cmov1819.p2photo.helpers.managers.LogManager.logInfo;
import static cmov1819.p2photo.helpers.managers.LogManager.logWarning;
import static cmov1819.p2photo.helpers.managers.LogManager.toast;
import static cmov1819.p2photo.helpers.termite.Consts.CATALOG_ID;
import static cmov1819.p2photo.helpers.termite.Consts.CATALOG_TITLE;
import static cmov1819.p2photo.helpers.termite.Consts.MEMBERS_PHOTOS;

public class CatalogMerge {
    private static final String CATALOG_MERGER_TAG = "CATALOG MERGER";
    private CatalogMerge() {
        // Does not allow this class to be instantiated. //
    }

    public static void mergeCatalogFiles(final Activity activity,
                                         final String catalogId,
                                         final JSONObject anotherCatalogFile) {

        // Retrieve catalog file contents as a JSON Object and compare them to the received catalog file
        try {
            // Load contents
            logInfo(CATALOG_MERGER_TAG, "Trying to merge catalog with" + catalogId + "...");
            String fileName = String.format("catalog_%s.json", catalogId);
            InputStream inputStream = activity.openFileInput(fileName);
            String thisCatalogFileContentsString = inputStreamToString(inputStream);
            JSONObject thisCatalogFileContents = new JSONObject(thisCatalogFileContentsString);
            JSONObject mergedContents =  mergeCatalogFileContents(thisCatalogFileContents, anotherCatalogFile);
            if (mergedContents == null) {
                toast(activity, "Couldn't update catalog file");
                logWarning(LogManager.NEW_CATALOG_SLICE_TAG, "Catalog merging resulted in null JSONObject");
            } else {
                FileOutputStream outputStream = activity.openFileOutput(fileName, Context.MODE_PRIVATE);
                outputStream.write(mergedContents.toString().getBytes());
                outputStream.close();
            }
        }

        catch (FileNotFoundException ex) {
            try {
                String fileName = String.format("catalog_%s.json", catalogId);
                FileOutputStream outputStream = activity.openFileOutput(fileName, Context.MODE_PRIVATE);
                outputStream.write(anotherCatalogFile.toString().getBytes());
                outputStream.close();
            }
            catch (IOException ex2) {
                logError(LogManager.NEW_CATALOG_TAG, ex2.getMessage());
                toast(activity, "Couldn't read stored catalog slice");
            }
        }
        catch (IOException | JSONException exc) {
            logError(LogManager.NEW_CATALOG_TAG, exc.getMessage());
            toast(activity, "Couldn't read stored catalog slice");
        }
    }

    private static JSONObject mergeCatalogFileContents(JSONObject thisFile, JSONObject otherFile) throws JSONException {
        String thisId = thisFile.getString(CATALOG_ID);
        String otherId = otherFile.getString(CATALOG_ID);

        if (!thisId.equals(otherId)) {
            return null;
        }

        JSONObject thisMembersPhotosMap = thisFile.getJSONObject(MEMBERS_PHOTOS);
        JSONObject anotherMembersPhotosMap = otherFile.getJSONObject(MEMBERS_PHOTOS);

        JSONObject mergedMembersPhotoMap = mergeMaps(thisMembersPhotosMap, anotherMembersPhotosMap);

        JSONObject mergedCatalogFileContents = new JSONObject();
        mergedCatalogFileContents.put(CATALOG_ID, thisId);
        mergedCatalogFileContents.put(CATALOG_TITLE, thisFile.getString(CATALOG_TITLE));
        mergedCatalogFileContents.put(MEMBERS_PHOTOS, mergedMembersPhotoMap);

        return mergedCatalogFileContents;
    }

    private static JSONObject mergeMaps(JSONObject thisMap, JSONObject receivedMap) {
        JSONObject mergedMap = new JSONObject();
        Iterator<String> receivedMembers = receivedMap.keys();
        while (receivedMembers.hasNext()) {
            String currentMember = receivedMembers.next();
            try {
                if (thisMap.has(currentMember)) {
                    List<String> thisCurrentMemberPhotos = ConvertUtils.jsonArrayToArrayList(receivedMap.getJSONArray(currentMember));
                    List<String> receivedCurrentMemberPhotos = ConvertUtils.jsonArrayToArrayList(thisMap.getJSONArray(currentMember));
                    thisCurrentMemberPhotos.addAll(receivedCurrentMemberPhotos);
                    List<String> mergedCurrentMemberPhotos = new ArrayList<>(new HashSet<>(thisCurrentMemberPhotos));
                    mergedMap.put(currentMember, mergedCurrentMemberPhotos);
                }
                else {
                    mergedMap.put(currentMember, receivedMap.getJSONArray(currentMember));
                }
            } catch (JSONException jsone) {
                // swallow
            }
        }
        return mergedMap;
    }

}
