package MobileAndUbiquitousComputing.P2Photos.Helpers;

import android.app.Activity;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import MobileAndUbiquitousComputing.P2Photos.DataObjects.RequestData;
import MobileAndUbiquitousComputing.P2Photos.DataObjects.ResponseData;
import MobileAndUbiquitousComputing.P2Photos.Exceptions.FailedOperationException;
import MobileAndUbiquitousComputing.P2Photos.Exceptions.NoResultsException;
import MobileAndUbiquitousComputing.P2Photos.MsgTypes.SuccessResponse;

public class FindUser {
    private FindUser() {
        // Prevents this class from being instantiated. //
    }

    public static LinkedHashMap<String, ArrayList> findUser(Activity activity, String usernameToFind, boolean bringAlbums)
            throws FailedOperationException, NoResultsException {
        Log.i("MSG", "Finding user " + usernameToFind + ".");
        String url = ConnectionManager.P2PHOTO_HOST + ConnectionManager.FIND_USERS_OPERATION + "?searchPattern="
                    + usernameToFind + "&bringAlbums=" + bringAlbums + "&calleeUsername=" + SessionManager.username;

        try {
            RequestData requestData = new RequestData(activity, RequestData.RequestType.FINDUSERS, url);

            ResponseData result = new QueryManager().execute(requestData).get();
            int code = result.getServerCode();
            // TODO - Response codes are now in a ResponseEntity. //
            if (code == 200) {
                Log.i("STATUS", "The find users operation was successful");

                SuccessResponse payload = (SuccessResponse) result.getPayload();
                Object object = payload.getResult();

                if (bringAlbums) {
                    LinkedHashMap<String, ArrayList> map = (LinkedHashMap<String, ArrayList>) object;
                    if (map.size() == 0) {
                        throw new NoResultsException();
                    }
                    return map;
                }
                else {
                    ArrayList<String> usernames = (ArrayList) object;
                    if (usernames.size() == 0) {
                        throw new NoResultsException();
                    }
                    LinkedHashMap<String, ArrayList> map = new LinkedHashMap<>();
                    for (String username : usernames) {
                        map.put(username, new ArrayList());
                    }
                    return map;
                }
            }
            else {
                Log.i("STATUS", "The find users operation was unsuccessful. Unknown error.");
                throw new FailedOperationException("URL: " + url);
            }
        }
        catch (ExecutionException | InterruptedException | ClassCastException ex) {
            throw new FailedOperationException(ex.getMessage());
        }
    }
}
