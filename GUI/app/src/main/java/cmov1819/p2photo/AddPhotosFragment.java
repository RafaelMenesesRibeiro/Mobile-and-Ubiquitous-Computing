package cmov1819.p2photo;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import cmov1819.p2photo.dataobjects.RequestData;
import cmov1819.p2photo.dataobjects.ResponseData;
import cmov1819.p2photo.exceptions.FailedOperationException;
import cmov1819.p2photo.helpers.architectures.CloudBackedArchitecture;
import cmov1819.p2photo.helpers.managers.ArchitectureManager;
import cmov1819.p2photo.helpers.managers.AuthStateManager;
import cmov1819.p2photo.helpers.managers.LogManager;
import cmov1819.p2photo.helpers.managers.QueryManager;
import cmov1819.p2photo.helpers.mediators.GoogleDriveMediator;
import cmov1819.p2photo.msgtypes.ErrorResponse;
import cmov1819.p2photo.msgtypes.SuccessResponse;

import static android.widget.Toast.LENGTH_LONG;
import static android.widget.Toast.LENGTH_SHORT;
import static cmov1819.p2photo.dataobjects.RequestData.RequestType.GET_GOOGLE_IDENTIFIERS;
import static cmov1819.p2photo.helpers.managers.SessionManager.getUsername;

public class AddPhotosFragment extends Fragment {
    public static final String CATALOG_ID_EXTRA = "catalogID";

    private View view;
    private ArrayList<String> catalogIDs;
    private Activity activity;
    private File selectedImage;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        this.activity = getActivity();
        final View inflaterView = view = inflater.inflate(R.layout.fragment_add_photos, container, false);
        populate(inflaterView);
        return inflaterView;
    }

    private void populate(final View view) {
        Button chooseButton = view.findViewById(R.id.choosePhoto);
        chooseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                choosePhotoClicked();
            }
        });
        Button doneButton = view.findViewById(R.id.done);
        MainMenuActivity.inactiveButton(doneButton);
        doneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addPhotosClicked(view);
            }
        });
        ImageView imageView = view.findViewById(R.id.imageView);
        imageView.setImageResource(R.drawable.img_not_available);

        Spinner membershipDropdown = view.findViewById(R.id.membershipDropdownMenu);
        catalogIDs = setDropdownAdapterAngGetCatalogIDs(activity, membershipDropdown);

        String catalogID;
        if (getArguments() != null && (catalogID = getArguments().getString(CATALOG_ID_EXTRA)) != null) {
            int index = catalogIDs.indexOf(catalogID);
            if (index != -1) {
                membershipDropdown.setSelection(index);
            }
        }
    }

    public void choosePhotoClicked() {
        Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, 0);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK) {
            Uri targetUri = data.getData();
            ImageView imageView = this.view.findViewById(R.id.imageView);
            Bitmap bitmap = null;

            try {
                bitmap = BitmapFactory.decodeStream(activity.getContentResolver().openInputStream(targetUri));
                imageView.setImageBitmap(bitmap);
            }
            catch (FileNotFoundException | NullPointerException ex) {
                imageView.setImageResource(R.drawable.img_not_available);
                String msg = "Could not load selected image file " + targetUri;
                LogManager.logError(LogManager.ADD_PHOTO_TAG, msg);
            }

            try {
                if (bitmap == null) {throw new IOException(); }
                save(bitmap);
            }
            catch (IOException ioex) {
                String msg = "Could not save selected image file " + targetUri;
                LogManager.logError(LogManager.ADD_PHOTO_TAG, msg);
                return;
            }

            Button doneButton = view.findViewById(R.id.done);
            MainMenuActivity.activateButton(doneButton);
        }
    }

    private void save(Bitmap bitmap) throws IOException {
        ContextWrapper cw = new ContextWrapper(getContext());
        File directory = cw.getDir("imageDir", Context.MODE_PRIVATE);
        File imageFile = new File(directory,"image.png");
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, new FileOutputStream(imageFile));
        selectedImage = imageFile;
    }


    public void addPhotosClicked(View view) {
        Spinner dropdown = view.findViewById(R.id.membershipDropdownMenu);
        String catalogId = catalogIDs.get(dropdown.getSelectedItemPosition());
        String catalogTitle = dropdown.getSelectedItem().toString();
        File androidFilePath = selectedImage;

        try {
            ArchitectureManager.systemArchitecture.addPhoto(getActivity(), catalogId, androidFilePath);
            LogManager.logAddPhoto();
            MainMenuActivity mainMenuActivity = (MainMenuActivity) activity;
            mainMenuActivity.goToCatalog(catalogId, catalogTitle);
        }
        catch (NullPointerException | ClassCastException ex) {
            LogManager.toast(activity, "Failed to add photo");
        }
        catch (FailedOperationException ex) {
            String msg = "Add Photo Operation unsuccessful";
            LogManager.logError(LogManager.ADD_PHOTO_TAG, msg);
        }
    }

    public static void addPhotoCloudArch(Activity activity, String catalogId, File androidFilePath)
            throws FailedOperationException{
        HashMap<String, String> googleDriveIdentifiers = getGoogleDriveIdentifiers(activity, catalogId);

        if (googleDriveIdentifiers == null) {
            String msg = "Failed to obtain googleDriveIdentifiers. Found ErrorResponse";
            LogManager.logError(LogManager.ADD_PHOTO_TAG, msg);
            LogManager.toast(activity, "Failed to add photo");
            throw new FailedOperationException();
        }

        Map.Entry<String, String> onlyEntry = googleDriveIdentifiers.entrySet().iterator().next();
        GoogleDriveMediator googleDriveMediator = ((CloudBackedArchitecture) ArchitectureManager.systemArchitecture).getGoogleDriveMediator(activity);
        AuthStateManager authStateManager = ((CloudBackedArchitecture) ArchitectureManager.systemArchitecture).getAuthStateManager(activity);
        googleDriveMediator.newPhoto(
                activity,
                onlyEntry.getKey(),
                onlyEntry.getValue(),
                androidFilePath.getName(),
                GoogleDriveMediator.TYPE_PNG,
                androidFilePath,
                authStateManager.getAuthState()
        );
    }

    private static HashMap<String, String> getGoogleDriveIdentifiers(Activity activity, String catalogId) throws FailedOperationException {
        try {
            String baseUrl = activity.getString(R.string.p2photo_host) + activity.getString(R.string.get_google_identifiers);

            String url = String.format(
                    "%s?calleeUsername=%s&catalogId=%s", baseUrl, getUsername(activity) , catalogId
            );

            RequestData requestData = new RequestData(activity, GET_GOOGLE_IDENTIFIERS, url);
            ResponseData result = new QueryManager().execute(requestData).get();

            int code = result.getServerCode();
            if (code != HttpURLConnection.HTTP_OK) {
                String reason = ((ErrorResponse) result.getPayload()).getReason();
                if (code == HttpURLConnection.HTTP_UNAUTHORIZED) {
                    LogManager.logError(LogManager.ADD_PHOTO_TAG, reason);
                    LogManager.toast(activity, "Session timed out, please login again");
                    activity.startActivity(new Intent(activity, LoginActivity.class));
                    return null;
                }
                else {
                    LogManager.logError(LogManager.ADD_PHOTO_TAG, reason);
                    LogManager.toast(activity, "Something went wrong");
                    return null;
                }
            }
            else {
                Object resultObject = ((SuccessResponse)result.getPayload()).getResult();
                return (HashMap<String, String>) resultObject;
            }
        }
        catch (ExecutionException | InterruptedException ex) {
            Thread.currentThread().interrupt();
            String msg = "Operation unsuccessful. " + ex.getMessage();
            LogManager.logError(LogManager.ADD_PHOTO_TAG, msg);
            throw new FailedOperationException(ex.getMessage());
        }
    }

    public static ArrayList<String> setDropdownAdapterAngGetCatalogIDs(Activity activity, Spinner dropdownMenu) {
        Map<String, String> map = ViewUserCatalogsFragment.getMemberships(activity);
        ArrayList<String> catalogTitles = new ArrayList<>();
        ArrayList<String> catalogIDs = new ArrayList<>();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            catalogIDs.add(entry.getKey());
            catalogTitles.add(entry.getValue());
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(activity, android.R.layout.simple_spinner_dropdown_item, catalogTitles);
        dropdownMenu.setAdapter(adapter);
        return catalogIDs;
    }
}
