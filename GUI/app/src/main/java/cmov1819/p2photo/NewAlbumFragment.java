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
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

import cmov1819.p2photo.dataobjects.PostRequestData;
import cmov1819.p2photo.dataobjects.RequestData;
import cmov1819.p2photo.dataobjects.ResponseData;
import cmov1819.p2photo.exceptions.FailedOperationException;
import cmov1819.p2photo.helpers.QueryManager;
import cmov1819.p2photo.helpers.SessionManager;
import cmov1819.p2photo.msgtypes.SuccessResponse;

public class NewAlbumFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_new_album, container, false);
        Button doneButton = view.findViewById(R.id.done);
        doneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                newAlbumClicked(view);
            }
        });
        return view;
    }

    public void newAlbumClicked(View view) {
        EditText titleInput = (EditText) view.findViewById(R.id.nameInputBox);
        String title = titleInput.getText().toString();

        if (title.equals("")) {
            Toast toast = Toast.makeText(this.getContext(), "Enter a name for the album", Toast.LENGTH_LONG);
            toast.show();
            return;
        }

        try {
            String catalogID = newAlbum(title);

            Fragment viewAlbumFragment = new ViewAlbumFragment();
            Bundle data = new Bundle();
            data.putString("catalogID", catalogID);
            data.putString("title", title);
            data.putStringArrayList("slices", new ArrayList<String>());
            viewAlbumFragment.setArguments(data);

            try {
                Activity activity = getActivity();
                MainMenuActivity mainMenuActivity = (MainMenuActivity) activity;
                mainMenuActivity.changeFragment(viewAlbumFragment, R.id.nav_view_album);
            }
            catch (NullPointerException | ClassCastException ex) {
                Toast.makeText(getContext(), "Could not present new album", Toast.LENGTH_LONG).show();
            }
        }
        catch (FailedOperationException foex) {
            Toast toast = Toast.makeText(this.getContext(), "The create album operation failed. Try again " +
                    "later", Toast.LENGTH_LONG);
            toast.show();
        }
    }

    private String newAlbum(String albumName) {
        Log.i("MSG", "Create album: " + albumName);
        String url = getString(R.string.p2photo_host) + getString(R.string.new_album_operation);

        try {
            JSONObject requestBody = new JSONObject();
            requestBody.put("catalogTitle", albumName);
            // TODO - Implement adding slice to Cloud Provider. //
            requestBody.put("sliceUrl", "http://www.acloudprovider.com/a_album_slice");
            requestBody.put("calleeUsername", SessionManager.getUsername(this.getActivity()));
            RequestData requestData = new PostRequestData(this.getActivity(), RequestData.RequestType.NEW_ALBUM
                    , url, requestBody);

            ResponseData result = new QueryManager().execute(requestData).get();
            int code = result.getServerCode();
            if (code == HttpURLConnection.HTTP_OK) {
                Log.i("STATUS", "The new album operation was successful");
                SuccessResponse payload = (SuccessResponse) result.getPayload();
                String catalogID = (String) payload.getResult();
                return catalogID;
            }
            else {
                Log.i("STATUS", "The new album operation was unsuccessful. Server response code: "
                        + code);
                throw new FailedOperationException();
            }
        }
        catch (JSONException | ExecutionException | InterruptedException ex) {
            throw new FailedOperationException(ex.getMessage());
        }
    }
}
