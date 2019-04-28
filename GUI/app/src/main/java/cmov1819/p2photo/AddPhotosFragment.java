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
import android.util.Log;
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
import cmov1819.p2photo.helpers.managers.AuthStateManager;
import cmov1819.p2photo.helpers.managers.QueryManager;
import cmov1819.p2photo.helpers.mediators.GoogleDriveMediator;
import cmov1819.p2photo.msgtypes.ErrorResponse;
import cmov1819.p2photo.msgtypes.SuccessResponse;

import static android.widget.Toast.LENGTH_LONG;
import static android.widget.Toast.LENGTH_SHORT;
import static cmov1819.p2photo.dataobjects.RequestData.RequestType.GET_GOOGLE_IDENTIFIERS;
import static cmov1819.p2photo.helpers.managers.SessionManager.getUsername;

public class AddPhotosFragment extends Fragment {
    private static final String ADD_PHOTO_TAG = "ADD PHOTO FRAGMENT";
    public static final String CATALOG_ID_EXTRA = "catalogID";

    private View view;

    private GoogleDriveMediator googleDriveMediator;
    private AuthStateManager authStateManager;

    private ArrayList<String> catalogIDs;
    private Activity activity;
    private File selectedImage;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        this.activity = getActivity();
        this.authStateManager = AuthStateManager.getInstance(this.getContext());
        this.googleDriveMediator = GoogleDriveMediator.getInstance(authStateManager.getAuthState().getAccessToken());

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
                Log.i("ERROR", "Add Photo: Could not load selected image file " + targetUri);
            }

            try {
                if (bitmap == null) {throw new IOException(); }
                save(bitmap);
            }
            catch (IOException ioex) {
                Log.i("ERROR", "Add Photo: Could not save selected image file " + targetUri);
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

        HashMap<String, String> googleDriveIdentifiers = getGoogleDriveIdentifiers(catalogId);

        if (googleDriveIdentifiers == null) {
            Log.e(ADD_PHOTO_TAG, "Failed to obtain googleDriveIdentifiers. Found ErrorResponse");
            Toast.makeText(activity, "Failed to add photo", LENGTH_SHORT).show();
            return;
        }

        Map.Entry<String, String> onlyEntry = googleDriveIdentifiers.entrySet().iterator().next();
        googleDriveMediator.newPhoto(
                getContext(),
                onlyEntry.getKey(),
                onlyEntry.getValue(),
                androidFilePath.getName(),
                GoogleDriveMediator.TYPE_PNG,
                androidFilePath,
                authStateManager.getAuthState()
        );

        try {
            MainMenuActivity mainMenuActivity = (MainMenuActivity) activity;
            mainMenuActivity.goToCatalog(catalogId, catalogTitle);
        }
        catch (NullPointerException | ClassCastException ex) {
            Toast.makeText(activity, "Failed to add photo", LENGTH_SHORT).show();
        }
    }

    private HashMap<String, String> getGoogleDriveIdentifiers(String catalogId) {
        try {
            Context context = getContext();
            String baseUrl = getString(R.string.p2photo_host) + getString(R.string.get_google_identifiers);

            String url = String.format(
                    "%s?calleeUsername=%s&catalogId=%s", baseUrl, getUsername(getActivity()) , catalogId
            );

            RequestData requestData = new RequestData(getActivity(), GET_GOOGLE_IDENTIFIERS, url);
            ResponseData result = new QueryManager().execute(requestData).get();

            int code = result.getServerCode();
            if (code != HttpURLConnection.HTTP_OK) {
                String reason = ((ErrorResponse) result.getPayload()).getReason();
                if (code == HttpURLConnection.HTTP_UNAUTHORIZED) {
                    Log.w(ADD_PHOTO_TAG, reason);
                    Toast.makeText(context, "Session timed out, please login again", LENGTH_SHORT).show();
                    context.startActivity(new Intent(context, LoginActivity.class));
                    return null;
                } else {
                    Log.e(ADD_PHOTO_TAG, reason);
                    Toast.makeText(context, "Something went wrong", LENGTH_LONG).show();
                    return null;
                }
            } else {
                Object resultObject = ((SuccessResponse)result.getPayload()).getResult();
                HashMap<String, String> googleIdentifiersMap = (HashMap<String, String>) resultObject;
                return googleIdentifiersMap;
            }
        } catch (ExecutionException | InterruptedException ex) {
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
