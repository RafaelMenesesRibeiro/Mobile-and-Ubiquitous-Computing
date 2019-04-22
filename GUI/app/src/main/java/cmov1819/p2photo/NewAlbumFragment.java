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

import org.json.JSONException;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.util.concurrent.ExecutionException;

import cmov1819.p2photo.dataobjects.PostRequestData;
import cmov1819.p2photo.dataobjects.RequestData;
import cmov1819.p2photo.dataobjects.ResponseData;
import cmov1819.p2photo.exceptions.FailedOperationException;
import cmov1819.p2photo.helpers.managers.QueryManager;
import cmov1819.p2photo.helpers.managers.SessionManager;
import cmov1819.p2photo.msgtypes.SuccessResponse;

public class NewAlbumFragment extends Fragment {
    private Activity activity;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        activity = getActivity();
        final View view = inflater.inflate(R.layout.fragment_new_album, container, false);
        populate(view);
        return view;
    }

    private void populate(final View view) {
        Button doneButton = view.findViewById(R.id.done);
        doneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                newAlbumClicked(view);
            }
        });
        EditText editText = view.findViewById(R.id.nameInputBox);
        MainMenuActivity.bingEditTextWithButton(editText, doneButton);
    }

    public void newAlbumClicked(View view) {
        EditText titleInput = view.findViewById(R.id.nameInputBox);
        String albumTitle = titleInput.getText().toString();

        if (albumTitle.equals("")) {
            Toast toast = Toast.makeText(this.getContext(), "Enter a name for the album", Toast.LENGTH_LONG);
            toast.show();
            return;
        }

        try {
            String albumID = newAlbum(albumTitle);
            MainMenuActivity mainMenuActivity = (MainMenuActivity) activity;
            mainMenuActivity.goToAlbum(albumID, albumTitle);
        }
        catch (FailedOperationException foex) {
            Toast.makeText(this.getContext(), "The create album operation failed. Try again later", Toast.LENGTH_LONG).show();
        }
        catch (NullPointerException | ClassCastException ex) {
        Toast.makeText(activity, "Could not present new album", Toast.LENGTH_LONG).show();
    }
    }

    private String newAlbum(String albumName) {
        Log.i("MSG", "Create album: " + albumName);
        String url = getString(R.string.p2photo_host) + getString(R.string.new_album_operation);

        try {
            JSONObject requestBody = new JSONObject();
            requestBody.put("catalogTitle", albumName);
            requestBody.put("sliceUrl", "http://www.acloudprovider.com/a_album_slice"); // TODO - Implement adding slice to Cloud Provider. //
            requestBody.put("calleeUsername", SessionManager.getUsername(activity));
            RequestData requestData = new PostRequestData(activity, RequestData.RequestType.NEW_ALBUM, url, requestBody);

            ResponseData result = new QueryManager().execute(requestData).get();
            int code = result.getServerCode();
            if (code == HttpURLConnection.HTTP_OK) {
                Log.i("STATUS", "The new album operation was successful");
                SuccessResponse payload = (SuccessResponse) result.getPayload();
                return (String) payload.getResult();
            }
            else {
                Log.i("STATUS", "The new album operation was unsuccessful. Server response code: " + code + ".\n" + result.getPayload().getMessage());
                throw new FailedOperationException();
            }
        }
        catch (JSONException | ExecutionException | InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new FailedOperationException(ex.getMessage());
        }
    }
}
