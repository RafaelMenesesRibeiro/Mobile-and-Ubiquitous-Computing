package MobileAndUbiquitousComputing.P2Photos.Helpers;

import android.app.Activity;
import android.util.Log;

import java.util.concurrent.ExecutionException;

import MobileAndUbiquitousComputing.P2Photos.DataObjects.RequestData;
import MobileAndUbiquitousComputing.P2Photos.DataObjects.ResponseData;
import MobileAndUbiquitousComputing.P2Photos.Exceptions.FailedOperationException;

public class FindUser {
    private FindUser() {
        // Prevents this class from being instantiated. //
    }

    public static void findUser(Activity activity, String usernameToFind, boolean bringAlbums) throws UnsupportedOperationException {
        Log.i("MSG", "Finding user " + usernameToFind + ".");
        String url = ConnectionManager.P2PHOTO_HOST + ConnectionManager.FIND_USERS_OPERATION + "?searchPatter="
                    + usernameToFind + "&bringAlbums=" + bringAlbums + "&calleeUsername=" + SessionManager.username;

        try {
            RequestData requestData = new RequestData(activity, RequestData.RequestType.FINDUSERS, url);

            ResponseData result = new QueryManager().execute(requestData).get();
            System.out.println("asdasdasdasdads22222222 " + result.toString());
            int code = result.getServerCode();
            // TODO - Response codes are now in a ResponseEntity. //
            if (code == 200) {
                Log.i("STATUS", "The find users operation was successful");
            }
            else {
                System.out.println("asdasdasdasdads " + result.toString());
                Log.i("STATUS", "The find users operation was unsuccessful. Unknown error.");
                throw new FailedOperationException("URL: " + url);
            }
        }
        catch (ExecutionException | InterruptedException ex) {
            throw new FailedOperationException(ex.getMessage());
        }

        /*
        RequestData requestData = new RequestData(RequestData.RequestType.GETFINDUSER, url);
        try {
            UsersResponseData result = (UsersResponseData) new QueryManager().execute(requestData).get();
            int code = result.getServerCode();
            if (code == 200) {
                Log.i("STATUS", "The find user operation was successful");
                List<UserData> usersData = result.getUsersList();
                for (UserData userData : usersData) {
                    System.out.println(usersData.toString());
                }
                System.out.println();
            }
            else {
                Log.i("STATUS", "The find user operation was unsuccessful. Unknown error.");
            }
        }
        catch (ClassCastException ccex) {
            ccex.printStackTrace();
        }
        catch (ExecutionException eex) {
            eex.printStackTrace();
        }
        catch (InterruptedException iex) {
            iex.printStackTrace();
        }
        */
    }
}
