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

import org.json.JSONException;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.util.concurrent.ExecutionException;

import cmov1819.p2photo.dataobjects.PostRequestData;
import cmov1819.p2photo.dataobjects.RequestData;
import cmov1819.p2photo.dataobjects.ResponseData;
import cmov1819.p2photo.exceptions.FailedOperationException;
import cmov1819.p2photo.exceptions.NoMembershipException;
import cmov1819.p2photo.exceptions.UsernameException;
import cmov1819.p2photo.helpers.QueryManager;
import cmov1819.p2photo.helpers.SessionManager;
import cmov1819.p2photo.msgtypes.ErrorResponse;

public class NewAlbumMemberFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_new_album_member, container, false);
        Button doneButton = view.findViewById(R.id.done);
        doneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addUserClicked(view);
            }
        });
        return view;
    }

    public void addUserClicked(View view) {
        EditText albumIDInput = view.findViewById(R.id.albumIDInputBox);
        EditText usernameInput = view.findViewById(R.id.toAddUsernameInputBox);
        String albumID = albumIDInput.getText().toString();
        String username = usernameInput.getText().toString();

        if (albumID.equals("") || username.equals("")) {
            albumIDInput.setText("");
            usernameInput.setText("");
        }

        try {
            addMember(albumID, username);
        } catch (FailedOperationException foex) {
            Toast toast = Toast.makeText(this.getContext(), "The add user to album operation failed. Try again" +
                    " later", Toast.LENGTH_LONG);
            toast.show();
        } catch (NoMembershipException nmex) {
            Toast toast = Toast.makeText(this.getContext(), "The add user to album operation failed. No " +
                    "membership", Toast.LENGTH_LONG);
            toast.show();
        } catch (UsernameException uex) {
            Toast toast = Toast.makeText(this.getContext(), "The add user to album operation failed. User does" +
                    " not exist", Toast.LENGTH_LONG);
            toast.show();
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
            requestBody.put("calleeUsername", SessionManager.getUsername(this.getActivity()));
            RequestData requestData = new PostRequestData(this.getActivity(),
                    RequestData.RequestType.NEW_ALBUM_MEMBER, url, requestBody);

            ResponseData result = new QueryManager().execute(requestData).get();
            int code = result.getServerCode();

            if (code == HttpURLConnection.HTTP_OK) {
                Log.i("STATUS", "The add user to album operation was successful");
            } else if (code == HttpURLConnection.HTTP_BAD_REQUEST) {
                ErrorResponse errorResponse = (ErrorResponse) result.getPayload();
                String reason = errorResponse.getReason();
                Log.i("STATUS", "The add user to album operation was unsuccessful. " +
                        "HTTP_BARD_REQUEST. Server response code: " + code + ".\n" + reason);
                throw new FailedOperationException(reason);
            } else if (code == HttpURLConnection.HTTP_UNAUTHORIZED) {
                ErrorResponse errorResponse = (ErrorResponse) result.getPayload();
                String reason = errorResponse.getReason();
                if (reason.equals(getString(R.string.no_membership))) {
                    Log.i("STATUS", "The add user to album operation was unsuccessful. " +
                            "Callee does not belong to album " + albumID);
                    throw new NoMembershipException(reason);
                }
            } else if (code == HttpURLConnection.HTTP_NOT_FOUND) {
                ErrorResponse errorResponse = (ErrorResponse) result.getPayload();
                String reason = errorResponse.getReason();
                if (reason.equals(getString(R.string.no_user))) {
                    Log.i("STATUS", "The add user to album operation was unsuccessful. " +
                            "Username does not exist " + username);
                    throw new UsernameException(reason);
                }
            } else {
                Log.i("STATUS", "The add user to album operation was unsuccessful. Server " +
                        "response code: " + code);
                throw new FailedOperationException();
            }
        } catch (JSONException | ExecutionException | InterruptedException ex) {
            throw new FailedOperationException(ex.getMessage());
        }
    }
}
