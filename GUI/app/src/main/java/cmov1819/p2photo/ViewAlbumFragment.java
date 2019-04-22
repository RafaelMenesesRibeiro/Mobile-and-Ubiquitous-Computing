package cmov1819.p2photo;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import cmov1819.p2photo.adapters.ImageGridAdapter;
import cmov1819.p2photo.dataobjects.RequestData;
import cmov1819.p2photo.dataobjects.ResponseData;
import cmov1819.p2photo.helpers.managers.AuthStateManager;
import cmov1819.p2photo.helpers.managers.QueryManager;
import cmov1819.p2photo.helpers.mediators.GoogleDriveMediator;
import cmov1819.p2photo.msgtypes.SuccessResponse;

import static cmov1819.p2photo.MainMenuActivity.HOME_SCREEN;
import static cmov1819.p2photo.MainMenuActivity.START_SCREEN;
import static cmov1819.p2photo.dataobjects.RequestData.RequestType.GET_CATALOG;
import static cmov1819.p2photo.helpers.managers.SessionManager.getUsername;

public class ViewAlbumFragment extends Fragment {
    public static final String ALBUM_ID_EXTRA = "catalogID";
    public static final String ALBUM_TITLE_EXTRA = "title";
    public static final String NO_ALBUM_SELECTED = "NO_ALBUM_SELECTED_ERROR";

    private Activity activity;
    private ArrayList<String> albumNames;
    private ArrayList<String> albumIDs;

    private GoogleDriveMediator googleDriveMediator;
    private AuthStateManager authStateManager;

    private String albumID;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        activity = getActivity();
        this.authStateManager = AuthStateManager.getInstance(this.getContext());
        this.googleDriveMediator = GoogleDriveMediator.getInstance(authStateManager.getAuthState().getAccessToken());

        final View view = inflater.inflate(R.layout.fragment_view_album, container, false);
        boolean couldPopulate = populate(view);
        if (!couldPopulate) {
            try {
                MainMenuActivity mainMenuActivity = (MainMenuActivity) activity;
                mainMenuActivity.goHome();
            }
            catch (ClassCastException ex) {
                Intent mainMenuActivityIntent = new Intent(activity, MainMenuActivity.class);
                mainMenuActivityIntent.putExtra(START_SCREEN, HOME_SCREEN);
                startActivity(mainMenuActivityIntent);
            }
        }

        Button addUserButton = view.findViewById(R.id.addUserButton);
        addUserButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addUserClicked();
            }
        });
        Button addPhotoButton = view.findViewById(R.id.addPhotoButton);
        addPhotoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addPhotoClicked();
            }
        });

        Button viewAlbumButton = view.findViewById(R.id.viewButton);
        viewAlbumButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ConstraintLayout constraintLayout = view.findViewById(R.id.dropdownContainer);
                constraintLayout.setVisibility(View.INVISIBLE);
                RelativeLayout relativeLayout = view.findViewById(R.id.albumViewContainer);
                relativeLayout.setVisibility(View.VISIBLE);

                Spinner dropdownMenu = view.findViewById(R.id.membershipDropdownMenu);
                int index = dropdownMenu.getSelectedItemPosition();
                albumID = albumIDs.get(index);
                String catalogName = albumNames.get(index);
                populateGrid(view, catalogName, getSlicesURLList(albumID));
            }
        });
        return view;
    }

    private boolean populate(View view) {
        if (getArguments() == null) {
            Log.i("ERROR", "VIEW ALBUM: arguments passed to fragment are null");
            return false;
        }

        albumID = getArguments().getString(ALBUM_ID_EXTRA);
        if (albumID == null) {
            Log.i("ERROR", "VIEW ALBUM: catalogID is null.");
            return false;
        }

        Map<String, String> map = ViewUserAlbumsFragment.getUserMemberships(activity);
        albumNames = new ArrayList<>();
        albumIDs = new ArrayList<>();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            albumIDs.add(entry.getKey());
            albumNames.add(entry.getValue());
        }

        if (albumID.equals(NO_ALBUM_SELECTED)) {
            RelativeLayout relativeLayout = view.findViewById(R.id.albumViewContainer);
            relativeLayout.setVisibility(View.INVISIBLE);
            ConstraintLayout constraintLayout = view.findViewById(R.id.dropdownContainer);
            constraintLayout.setVisibility(View.VISIBLE);

            Spinner membershipDropdown = view.findViewById(R.id.membershipDropdownMenu);
            ArrayAdapter<String> adapter = new ArrayAdapter<>(activity, android.R.layout.simple_spinner_dropdown_item, albumNames);
            membershipDropdown.setAdapter(adapter);
            return true;
        }

        String catalogTitle = getArguments().getString(ALBUM_TITLE_EXTRA);
        if (catalogTitle == null) {
            Log.i("ERROR", "VIEW ALBUM: catalogTitle is null.");
            return false;
        }
        List<String[]> slicesURLList = getSlicesURLList(albumID);
        populateGrid(view, catalogTitle, slicesURLList);
        return true;
    }

    private void populateGrid(View view, String catalogTitle, List<String[]> slicesURLList) {
        TextView catalogTitleTextView = view.findViewById(R.id.albumTitleLabel);
        catalogTitleTextView.setText(catalogTitle);

        for (String[] sliceInfo : slicesURLList) {
            googleDriveMediator.retrieveCatalogSlice(getContext(), view, sliceInfo[1], authStateManager.getAuthState());
            return;
        }
    }

    private void addUserClicked() {
        try {
            MainMenuActivity mainMenuActivity = (MainMenuActivity) activity;
            mainMenuActivity.goToAddUser(albumID);
        }
        catch (NullPointerException | ClassCastException ex) {
            Toast.makeText(getContext(), "Could not present add new member screen", Toast.LENGTH_LONG).show();
        }
    }

    private void addPhotoClicked() {
        try {
            MainMenuActivity mainMenuActivity = (MainMenuActivity) activity;
            mainMenuActivity.goToAddPhoto(albumID);
        }
        catch (NullPointerException | ClassCastException ex) {
            Toast.makeText(getContext(), "Could not present add new photo screen", Toast.LENGTH_LONG).show();
        }
    }

    public List<String[]> getSlicesURLList(String catalogID) {
        String url = getString(
                R.string.view_album_endpoint) +
                "?calleeUsername=" + getUsername(activity) + "&catalogId=" + catalogID;

        try {
            RequestData requestData = new RequestData(getActivity(), GET_CATALOG, url);
            ResponseData responseData = new QueryManager().execute(requestData).get();
            if (responseData.getServerCode() == HttpURLConnection.HTTP_OK) {
                SuccessResponse payload = (SuccessResponse) responseData.getPayload();
                return (ArrayList<String[]>) payload.getResult();
            }
        }
        catch (ExecutionException | InterruptedException ex) {
            Thread.currentThread().interrupt();
            Log.i("ERROR", "VIEW ALBUM: " + ex.getMessage());
        }
        return new ArrayList<>();
    }
}
