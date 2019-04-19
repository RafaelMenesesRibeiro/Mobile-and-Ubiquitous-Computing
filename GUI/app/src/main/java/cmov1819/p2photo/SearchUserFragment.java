package cmov1819.p2photo;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.concurrent.ExecutionException;

import cmov1819.p2photo.R;
import cmov1819.p2photo.dataobjects.RequestData;
import cmov1819.p2photo.dataobjects.ResponseData;
import cmov1819.p2photo.exceptions.BadInputException;
import cmov1819.p2photo.exceptions.FailedOperationException;
import cmov1819.p2photo.exceptions.NoResultsException;
import cmov1819.p2photo.helpers.QueryManager;
import cmov1819.p2photo.helpers.SessionManager;
import cmov1819.p2photo.msgtypes.SuccessResponse;

public class SearchUserFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_search_user, container, false);
    }

    public void SearchUser(View view) throws BadInputException {
        String username = ((EditText) view.findViewById(R.id.usernameInputBox)).getText().toString();
        if (username.equals("")) {
            // TODO - Handle this. //
            throw new BadInputException("The username to find cannot be empty.");
        }
        try {
            // TODO - Design tick box for 'bringAlmbums'. //
            LinkedHashMap<String, ArrayList> usernames = searchUser(username, true);
        } catch (NoResultsException nrex) {
            Toast toast = Toast.makeText(this.getContext(), "No results were found", Toast.LENGTH_LONG);
            toast.show();
        } catch (FailedOperationException foex) {
            Toast toast = Toast.makeText(this.getContext(), "The find users operation failed. Try again later"
                    , Toast.LENGTH_LONG);
            toast.show();
        }
    }

    public LinkedHashMap<String, ArrayList> searchUser(String usernameToFind, boolean bringAlbums)
            throws FailedOperationException, NoResultsException {
        Log.i("MSG", "Finding user " + usernameToFind + ".");
        String url =
                getString(R.string.p2photo_host) + getString(R.string.find_users_operation) +
                        "?searchPattern="
                        + usernameToFind + "&bringAlbums=" + bringAlbums + "&calleeUsername=" + SessionManager.getUsername(this.getActivity());

        try {
            RequestData requestData = new RequestData(this.getActivity(), RequestData.RequestType.SEARCH_USERS,
                    url);

            ResponseData result = new QueryManager().execute(requestData).get();
            int code = result.getServerCode();
            if (code == HttpURLConnection.HTTP_OK) {
                Log.i("STATUS", "The find users operation was successful");

                SuccessResponse payload = (SuccessResponse) result.getPayload();
                Object object = payload.getResult();

                if (bringAlbums) {
                    LinkedHashMap<String, ArrayList> map =
                            (LinkedHashMap<String, ArrayList>) object;
                    Log.i("MSG", "Users and respective albums: " + map.toString());
                    if (map.size() == 0) {
                        throw new NoResultsException();
                    }
                    return map;
                } else {
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
            } else {
                Log.i("STATUS", "The find users operation was unsuccessful. Server response code:" +
                        " " + code);
                throw new FailedOperationException("URL: " + url);
            }
        } catch (ExecutionException | InterruptedException | ClassCastException ex) {
            throw new FailedOperationException(ex.getMessage());
        }
    }
}
