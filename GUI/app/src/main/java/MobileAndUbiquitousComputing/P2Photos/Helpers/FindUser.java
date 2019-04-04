package MobileAndUbiquitousComputing.P2Photos.Helpers;

import android.util.Log;

import java.util.List;
import java.util.concurrent.ExecutionException;

import MobileAndUbiquitousComputing.P2Photos.DataObjects.RequestData;
import MobileAndUbiquitousComputing.P2Photos.DataObjects.UserData;
import MobileAndUbiquitousComputing.P2Photos.DataObjects.UsersResponseData;

public class FindUser {
    public static void FindUser(String usernameToFind, boolean bringAlbums) throws UnsupportedOperationException {
        Log.i("MSG", "Finding user " + usernameToFind + ".");
        String url = "http://p2photo-production.herokuapp.com/findUsers?searchPatter=" +
                usernameToFind + "&bringAlbums=" + bringAlbums + "&calleeUsername=" + Login.getUsername();
        // TODO - Needs redoing. //
        throw new UnsupportedOperationException();
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
