package cmov1819.p2photo;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import cmov1819.p2photo.dataobjects.PostRequestData;
import cmov1819.p2photo.dataobjects.RequestData;
import cmov1819.p2photo.dataobjects.ResponseData;
import cmov1819.p2photo.exceptions.FailedOperationException;
import cmov1819.p2photo.exceptions.NoMembershipException;
import cmov1819.p2photo.exceptions.UsernameException;
import cmov1819.p2photo.helpers.managers.QueryManager;
import cmov1819.p2photo.helpers.managers.SessionManager;
import cmov1819.p2photo.msgtypes.ErrorResponse;

import static cmov1819.p2photo.ViewAlbumFragment.CATALOG_ID_EXTRA;
import static cmov1819.p2photo.ViewAlbumFragment.TITLE_EXTRA;

public class NewAlbumMemberFragment extends Fragment {
    public static final String ALBUM_ID_EXTRA = "albumID";
    private ArrayList<String> albumIDs;
    private Activity activity;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        activity = getActivity();
        final View view = inflater.inflate(R.layout.fragment_new_album_member, container, false);
        final Button doneButton = view.findViewById(R.id.done);
        doneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addUserClicked(view);
            }
        });
        MainMenuActivity.InactiveButton(doneButton);
        final EditText editText = view.findViewById(R.id.toAddUsernameInputBox);
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { /* Do nothing */ }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (editText.getText().toString().isEmpty()) {
                    MainMenuActivity.InactiveButton(doneButton);
                    return;
                }
                MainMenuActivity.ActivateButton(doneButton);
            }

            @Override
            public void afterTextChanged(Editable s) { /* Do nothing */ }
        });
        populate(view);
        return view;
    }

    private void populate(View view) {
        Spinner membershipDropdown = view.findViewById(R.id.membershipDropdownMenu);
        Map<String, String> map = ViewUserAlbumsFragment.getUserMemberships(activity);
        ArrayList<String> albumNames = new ArrayList<>();
        albumIDs = new ArrayList<>();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            albumIDs.add(entry.getKey());
            albumNames.add(entry.getValue());
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(activity, android.R.layout.simple_spinner_dropdown_item, albumNames);
        membershipDropdown.setAdapter(adapter);

        String albumID;
        if (getArguments() != null && (albumID = getArguments().getString(ALBUM_ID_EXTRA)) != null) {
            int index = albumIDs.indexOf(albumID);
            if (index != -1) {
                membershipDropdown.setSelection(index);
            }
        }
    }

    public void addUserClicked(View view) {
        Spinner albumIDInput = view.findViewById(R.id.membershipDropdownMenu);
        String albumID = albumIDs.get(albumIDInput.getSelectedItemPosition());
        String albumName = albumIDInput.getSelectedItem().toString();
        EditText usernameInput = view.findViewById(R.id.toAddUsernameInputBox);
        String username = usernameInput.getText().toString();

        if (username.equals("")) {
            Toast.makeText(getContext(), "The username cannot by empty", Toast.LENGTH_LONG).show();
            return;
        }

        try {
            addMember(albumID, username);

            Fragment viewAlbumFragment = new ViewAlbumFragment();
            Bundle data = new Bundle();
            data.putString(CATALOG_ID_EXTRA, albumID);
            data.putString(TITLE_EXTRA, albumName);
            viewAlbumFragment.setArguments(data);

            MainMenuActivity mainMenuActivity = (MainMenuActivity) activity;
            mainMenuActivity.changeFragment(viewAlbumFragment, R.id.nav_view_album);
        }
        catch (FailedOperationException foex) {
            Toast.makeText(activity, "The add user to album operation failed. Try again later", Toast.LENGTH_LONG).show();
        }
        catch (NoMembershipException nmex) {
            Toast.makeText(activity, "The add user to album operation failed. No membership", Toast.LENGTH_LONG).show();
        }
        catch (UsernameException uex) {
            Toast.makeText(activity, "The add user to album operation failed. User does not exist", Toast.LENGTH_LONG).show();
        }
        catch (NullPointerException | ClassCastException ex) {
            Toast.makeText(activity, "Could not present new album", Toast.LENGTH_LONG).show();
        }
    }

    private void addMember(String albumID, String username)
            throws FailedOperationException, NoMembershipException, UsernameException {
        Log.i("MSG", "Add User to Album: " + albumID + ", " + username);
        String url = getString(R.string.p2photo_host) + getString(R.string.add_member_operation);

        try {
            JSONObject requestBody = new JSONObject();
            requestBody.put("catalogId", albumID);
            requestBody.put("newMemberUsername", username);
            requestBody.put("calleeUsername", SessionManager.getUsername(activity));
            RequestData requestData = new PostRequestData(this.getActivity(),
                    RequestData.RequestType.NEW_ALBUM_MEMBER, url, requestBody);

            ResponseData result = new QueryManager().execute(requestData).get();
            int code = result.getServerCode();

            if (code == HttpURLConnection.HTTP_OK) {
                Log.i("STATUS", "The add user to album operation was successful");
            }
            else if (code == HttpURLConnection.HTTP_BAD_REQUEST) {
                ErrorResponse errorResponse = (ErrorResponse) result.getPayload();
                String reason = errorResponse.getReason();
                Log.i("STATUS", getString(R.string.add_user_unsuccessful) + reason);
                throw new FailedOperationException(reason);
            }
            else if (code == HttpURLConnection.HTTP_UNAUTHORIZED) {
                ErrorResponse errorResponse = (ErrorResponse) result.getPayload();
                String reason = errorResponse.getReason();
                if (reason.equals(getString(R.string.no_membership))) {
                    Log.i("STATUS", getString(R.string.add_user_unsuccessful) + "Callee does not belong to album " + albumID);
                    throw new NoMembershipException(reason);
                }
            }
            else if (code == HttpURLConnection.HTTP_NOT_FOUND) {
                ErrorResponse errorResponse = (ErrorResponse) result.getPayload();
                String reason = errorResponse.getReason();
                if (reason.equals(getString(R.string.no_user))) {
                    Log.i("STATUS", getString(R.string.add_user_unsuccessful) + "Username does not exist " + username);
                    throw new UsernameException(reason);
                }
            }
            else {
                Log.i("STATUS", R.string.add_user_unsuccessful + "Server response code: " + code);
                throw new FailedOperationException();
            }
        }
        catch (JSONException | ExecutionException | InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new FailedOperationException(ex.getMessage());
        }
    }
}
