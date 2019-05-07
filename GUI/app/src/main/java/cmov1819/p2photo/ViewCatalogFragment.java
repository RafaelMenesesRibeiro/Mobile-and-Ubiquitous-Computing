package cmov1819.p2photo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import cmov1819.p2photo.adapters.ImageGridAdapter;
import cmov1819.p2photo.dataobjects.RequestData;
import cmov1819.p2photo.dataobjects.ResponseData;
import cmov1819.p2photo.helpers.architectures.CloudBackedArchitecture;
import cmov1819.p2photo.helpers.managers.ArchitectureManager;
import cmov1819.p2photo.helpers.managers.AuthStateManager;
import cmov1819.p2photo.helpers.managers.LogManager;
import cmov1819.p2photo.helpers.managers.QueryManager;
import cmov1819.p2photo.helpers.mediators.GoogleDriveMediator;
import cmov1819.p2photo.msgtypes.SuccessResponse;

import static cmov1819.p2photo.MainMenuActivity.HOME_SCREEN;
import static cmov1819.p2photo.MainMenuActivity.START_SCREEN;
import static cmov1819.p2photo.dataobjects.RequestData.RequestType.GET_CATALOG;
import static cmov1819.p2photo.helpers.managers.SessionManager.getUsername;

public class ViewCatalogFragment extends Fragment {
    public static final String CATALOG_ID_EXTRA = "catalogID";
    public static final String CATALOG_TITLE_EXTRA = "title";
    public static final String NO_CATALOG_SELECTED = "NO_CATALOG_SELECTED_ERROR";

    private Activity activity;
    private ArrayList<String> catalogTitles;
    private ArrayList<String> catalogIDs;
    private String catalogID;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        activity = getActivity();
        final View view = inflater.inflate(R.layout.fragment_view_catalog, container, false);
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

        Button viewCatalogButton = view.findViewById(R.id.viewButton);
        viewCatalogButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ConstraintLayout constraintLayout = view.findViewById(R.id.dropdownContainer);
                constraintLayout.setVisibility(View.INVISIBLE);
                RelativeLayout relativeLayout = view.findViewById(R.id.catalogViewContainer);
                relativeLayout.setVisibility(View.VISIBLE);

                Spinner dropdownMenu = view.findViewById(R.id.membershipDropdownMenu);
                int index = dropdownMenu.getSelectedItemPosition();
                catalogID = catalogIDs.get(index);
                String catalogTitle = catalogTitles.get(index);
                ArchitectureManager.systemArchitecture.viewCatalog(activity, view, catalogID, catalogTitle);
            }
        });
        return view;
    }

    private boolean populate(View view) {
        if (getArguments() == null) {
            String msg = "Arguments passed to fragment are null";
            LogManager.logError(LogManager.VIEW_CATALOG_TAG, msg);
            return false;
        }

        catalogID = getArguments().getString(CATALOG_ID_EXTRA);
        if (catalogID == null) {
            String msg = "catalogID is null";
            LogManager.logError(LogManager.VIEW_CATALOG_TAG, msg);
            return false;
        }

        Map<String, String> map = ViewUserCatalogsFragment.getMemberships(activity);
        catalogTitles = new ArrayList<>();
        catalogIDs = new ArrayList<>();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            catalogIDs.add(entry.getKey());
            catalogTitles.add(entry.getValue());
        }

        if (catalogID.equals(NO_CATALOG_SELECTED)) {
            RelativeLayout relativeLayout = view.findViewById(R.id.catalogViewContainer);
            relativeLayout.setVisibility(View.INVISIBLE);
            ConstraintLayout constraintLayout = view.findViewById(R.id.dropdownContainer);
            constraintLayout.setVisibility(View.VISIBLE);

            Spinner membershipDropdown = view.findViewById(R.id.membershipDropdownMenu);
            ArrayAdapter<String> adapter = new ArrayAdapter<>(activity, android.R.layout.simple_spinner_dropdown_item, catalogTitles);
            membershipDropdown.setAdapter(adapter);
            return true;
        }

        String catalogTitle = getArguments().getString(CATALOG_TITLE_EXTRA);
        if (catalogTitle == null) {
            String msg = "catalogTitle is null.";
            LogManager.logError(LogManager.VIEW_CATALOG_TAG, msg);
            return false;
        }

        ArchitectureManager.systemArchitecture.viewCatalog(activity, view, catalogID, catalogTitle);
        return true;
    }

    public static void populateGridCloudArch(Activity activity, View view, String catalogID, String catalogTitle) {
        List<String>  googleSliceFileIdentifiersList = getGoogleSliceFileIdentifiersList(activity, catalogID);
        TextView catalogTitleTextView = view.findViewById(R.id.catalogTitleLabel);
        catalogTitleTextView.setText(catalogTitle);

        GoogleDriveMediator googleDriveMediator = ((CloudBackedArchitecture) ArchitectureManager.systemArchitecture).getGoogleDriveMediator(activity);
        AuthStateManager authStateManager = ((CloudBackedArchitecture) ArchitectureManager.systemArchitecture).getAuthStateManager(activity);

        for (String googleCatalogFileId : googleSliceFileIdentifiersList) {
            googleDriveMediator.viewCatalogSlicePhotos(activity, view, googleCatalogFileId, authStateManager.getAuthState()
            );
        }
        LogManager.logViewCatalog(catalogID, catalogTitle);
    }

    public static void drawImages(View view, final Context context, List<Bitmap> contents) {
        GridView grid = view.findViewById(R.id.catalogGrid);
        Adapter adapter = grid.getAdapter();
        if (adapter != null) {
            try {
                List<Bitmap> newContents = ((ImageGridAdapter) grid.getAdapter()).getContents();
                contents.addAll(newContents);
            }
            catch (ClassCastException ex) {
                // Do nothing. Just doesn't add the old photos.
            }
        }
        grid.setAdapter(new ImageGridAdapter(context, contents));
    }

    private void addUserClicked() {
        try {
            MainMenuActivity mainMenuActivity = (MainMenuActivity) activity;
            mainMenuActivity.goToAddUser(catalogID);
        }
        catch (NullPointerException | ClassCastException ex) {
            LogManager.toast(getActivity(),"Could not present add new member screen");
        }
    }

    private void addPhotoClicked() {
        try {
            MainMenuActivity mainMenuActivity = (MainMenuActivity) activity;
            mainMenuActivity.goToAddPhoto(catalogID);
        }
        catch (NullPointerException | ClassCastException ex) {
            LogManager.toast(getActivity(), "Could not present add new photo screen");
        }
    }

    public static List<String> getGoogleSliceFileIdentifiersList(Activity activity, String catalogID) {
        String url = activity.getString(R.string.p2photo_host) + activity.getString(R.string.view_catalog) +
                "?calleeUsername=" + getUsername(activity) + "&catalogId=" + catalogID;

        try {
            RequestData requestData = new RequestData(activity, GET_CATALOG, url);
            ResponseData responseData = new QueryManager().execute(requestData).get();
            if (responseData.getServerCode() == HttpURLConnection.HTTP_OK) {
                SuccessResponse payload = (SuccessResponse) responseData.getPayload();
                return (List<String> ) payload.getResult();
            }
        }
        catch (ExecutionException | InterruptedException ex) {
            Thread.currentThread().interrupt();
            LogManager.logError(LogManager.VIEW_CATALOG_TAG, ex.getMessage());
        }
        return new ArrayList<>();
    }
}
