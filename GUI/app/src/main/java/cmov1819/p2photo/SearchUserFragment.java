package cmov1819.p2photo;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

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
import cmov1819.p2photo.helpers.mediators.P2PWebServerMediator;
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
            LogManager.toast(this.getActivity(), "The username cannot be empty.");
            return;
        }
        try {
            Map<String, ArrayList> usernames = searchUser(username);
            LogManager.logSearchUser(username);
            MainMenuActivity mainMenuActivity = (MainMenuActivity) activity;
            mainMenuActivity.goToListUsers(new ArrayList<>(usernames.keySet()));
        }
        catch (NoResultsException nrex) {
            LogManager.toast(activity, "No results were found");
        }
        catch (FailedOperationException foex) {
            LogManager.toast(activity, "No users found");
        }
        catch (NullPointerException | ClassCastException ex) {
            LogManager.toast(activity, "Could not present users list");
        }
    }

    public Map<String, ArrayList> searchUser(String usernameToFind)
            throws FailedOperationException, NoResultsException {
        String url = getString(R.string.p2photo_host) + getString(R.string.find_users) +
                    "?searchPattern=" + usernameToFind + "&bringCatalogs=" + true
                    + "&calleeUsername=" + SessionManager.getUsername(activity);

        try {
            RequestData requestData = new RequestData(this.getActivity(), RequestData.RequestType.SEARCH_USERS, url);
            ResponseData result = new P2PWebServerMediator().execute(requestData).get();
            int code = result.getServerCode();
            if (code == HttpURLConnection.HTTP_OK) {
                SuccessResponse payload = (SuccessResponse) result.getPayload();
                LinkedHashMap<String, ArrayList> map = (LinkedHashMap<String, ArrayList>) payload.getResult();
                String msg = "Users and respective catalogs: " + map.toString();
                LogManager.logInfo(LogManager.SEARCH_USER_TAG, msg);
                if (map.size() == 0) {
                    throw new NoResultsException();
                }
                return map;
            }
            else {
                String msg = getString(R.string.find_user_unsuccessful) + "Server response code: " + code;
                LogManager.logError(LogManager.SEARCH_USER_TAG, msg);
                throw new FailedOperationException("URL: " + url);
            }
        }
        catch (ClassCastException ccex) {
            String msg = getString(R.string.find_user_unsuccessful) + "Caught Class Cast Exception.";
            LogManager.logError(LogManager.SEARCH_USER_TAG, msg);
            throw new NoResultsException(ccex.getMessage());
        }
        catch (ExecutionException | InterruptedException ex) {
            Thread.currentThread().interrupt();
            String msg = getString(R.string.find_user_unsuccessful);
            LogManager.logError(LogManager.SEARCH_USER_TAG, msg);
            throw new FailedOperationException(ex.getMessage());
        }
    }
}
