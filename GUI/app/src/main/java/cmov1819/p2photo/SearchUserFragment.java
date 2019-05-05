package cmov1819.p2photo;

import android.app.Activity;
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
import java.util.Map;
import java.util.concurrent.ExecutionException;

import cmov1819.p2photo.dataobjects.RequestData;
import cmov1819.p2photo.dataobjects.ResponseData;
import cmov1819.p2photo.exceptions.BadInputException;
import cmov1819.p2photo.exceptions.FailedOperationException;
import cmov1819.p2photo.exceptions.NoResultsException;
import cmov1819.p2photo.helpers.managers.LogManager;
import cmov1819.p2photo.helpers.managers.QueryManager;
import cmov1819.p2photo.helpers.managers.SessionManager;
import cmov1819.p2photo.msgtypes.SuccessResponse;

public class SearchUserFragment extends Fragment {
    private Activity activity;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        activity = getActivity();
        final View view = inflater.inflate(R.layout.fragment_search_user, container, false);
        Button doneButton = view.findViewById(R.id.done);
        doneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchUserClicked(view);
            }
        });
        EditText editText = view.findViewById(R.id.usernameInputBox);
        MainMenuActivity.bingEditTextWithButton(editText, doneButton);
        return view;
    }

    public void searchUserClicked(View view) throws BadInputException {
        String username = ((EditText) view.findViewById(R.id.usernameInputBox)).getText().toString();
        if (username.equals("")) {
            Toast.makeText(this.getContext(), "The username cannot be empty.", Toast.LENGTH_LONG).show();
            return;
        }
        try {
            Map<String, ArrayList> usernames = searchUser(username);
            LogManager.LogSearchUser(username);
            MainMenuActivity mainMenuActivity = (MainMenuActivity) activity;
            mainMenuActivity.goToListUsers(new ArrayList<>(usernames.keySet()));
        }
        catch (NoResultsException nrex) {
            Toast.makeText(activity, "No results were found", Toast.LENGTH_LONG).show();
        }
        catch (FailedOperationException foex) {
            Toast.makeText(activity, "No users found", Toast.LENGTH_LONG).show();
        }
        catch (NullPointerException | ClassCastException ex) {
            Toast.makeText(activity, "Could not present users list", Toast.LENGTH_LONG).show();
        }
    }

    public Map<String, ArrayList> searchUser(String usernameToFind)
            throws FailedOperationException, NoResultsException {
        Log.i("MSG", "Finding user " + usernameToFind + ".");
        String url = getString(R.string.p2photo_host) + getString(R.string.find_users) +
                    "?searchPattern=" + usernameToFind + "&bringCatalogs=" + true
                    + "&calleeUsername=" + SessionManager.getUsername(activity);

        try {
            RequestData requestData = new RequestData(this.getActivity(), RequestData.RequestType.SEARCH_USERS, url);
            ResponseData result = new QueryManager().execute(requestData).get();
            int code = result.getServerCode();
            if (code == HttpURLConnection.HTTP_OK) {
                Log.i("STATUS", "The find users operation was successful");

                SuccessResponse payload = (SuccessResponse) result.getPayload();
                LinkedHashMap<String, ArrayList> map = (LinkedHashMap<String, ArrayList>) payload.getResult();
                Log.i("MSG", "Users and respective catalogs: " + map.toString());
                if (map.size() == 0) {
                    throw new NoResultsException();
                }
                return map;
            }
            else {
                Log.i("ERROR", "SEARCH USER: " + getString(R.string.find_user_unsuccessful) + "Server response code: " + code);
                throw new FailedOperationException("URL: " + url);
            }
        }
        catch (ClassCastException ccex) {
            Log.i("ERROR", "SEARCH USER: " + getString(R.string.find_user_unsuccessful) + "Caught Class Cast Exception.");
            throw new NoResultsException(ccex.getMessage());
        }
        catch (ExecutionException | InterruptedException ex) {
            Thread.currentThread().interrupt();
            Log.i("ERROR", "SEARCH USER: " + getString(R.string.find_user_unsuccessful));
            throw new FailedOperationException(ex.getMessage());
        }
    }
}
