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
import java.util.ArrayList;
import java.util.Map;

import cmov1819.p2photo.helpers.managers.AuthStateManager;
import cmov1819.p2photo.helpers.mediators.GoogleDriveMediator;

public class AddPhotosFragment extends Fragment {
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
        String catalogID = catalogIDs.get(dropdown.getSelectedItemPosition());
        String catalogTitle = dropdown.getSelectedItem().toString();
        File androidFilePath = selectedImage;

        /* TODO Raphael:
        * If we want to store photos in their folders along side their catalog.json file, we need to
        * the googleDriveCatalogJsonId as well as the googleDriveCatalogFolderId. These can be stored seperatly on the
        * database or they can be stored together using concatenation of strings splited by :p2pspliter:.
        * The alternative is to store googleDriveCatalogJsonId inside googleDriveCatalogFolderId, but store all photos
        * in the root folder of the user. In this case we only need to store googleDriveCatalogJsonId in the database
        */
        googleDriveMediator.newPhoto(
                getContext(),
                "1Uy1i200TF8sPiccjnhgjDCD3RfpEpHZB",
                "1NQeZnrE9vLldHvOwh3PEomLE5CozdHmR",
                androidFilePath.getName(),
                GoogleDriveMediator.TYPE_PNG,
                androidFilePath,
                authStateManager.getAuthState()
        );

        // TODO Bambi - Call method in GoogleDriveManager that: adds the photo's URL to user's catalog file

        try {
            MainMenuActivity mainMenuActivity = (MainMenuActivity) activity;
            mainMenuActivity.goToCatalog(catalogID, catalogTitle);
        }
        catch (NullPointerException | ClassCastException ex) {
            Toast.makeText(activity, "Could not present new album", Toast.LENGTH_LONG).show();
        }
    }

    public static ArrayList<String> setDropdownAdapterAngGetCatalogIDs(Activity activity, Spinner dropdownMenu) {
        Map<String, String> map = ViewUserCatalogsFragment.getUserMemberships(activity);
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
