package cmov1819.p2photo;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Map;

public class AddPhotosFragment extends Fragment {
    public static final String ALBUM_ID_EXTRA = "albumID";
    private Activity activity;
    private View view;
    private ArrayList<String> albumIDs;
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
        albumIDs = setDropdownAdapterAngGetAlbumIDs(activity, membershipDropdown);

        String albumID;
        if (getArguments() != null && (albumID = getArguments().getString(ALBUM_ID_EXTRA)) != null) {
            int index = albumIDs.indexOf(albumID);
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
            try {
                Bitmap bitmap = BitmapFactory.decodeStream(activity.getContentResolver().openInputStream(targetUri));
                imageView.setImageBitmap(bitmap);

                try {
                    save(bitmap);
                    Bitmap bitmap2 = BitmapFactory.decodeFile(selectedImage.getAbsolutePath());
                    imageView.setImageBitmap(bitmap2);
                }
                catch (IOException ioex) {
                    Log.i("ERROR", "Add Photo: Could not upload selected image file " + targetUri);
                    return;
                }

                Button doneButton = view.findViewById(R.id.done);
                MainMenuActivity.activateButton(doneButton);
            }
            catch (FileNotFoundException | NullPointerException ex) {
                imageView.setImageResource(R.drawable.img_not_available);
                Log.i("ERROR", "Add Photo: Could not load selected image file " + targetUri);
                ex.printStackTrace();
            }
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
        String albumID = albumIDs.get(dropdown.getSelectedItemPosition());
        String albumName = dropdown.getSelectedItem().toString();
        ImageView imageView = view.findViewById(R.id.imageView);
        Bitmap bitmap = ((BitmapDrawable) imageView.getDrawable()).getBitmap();
        /* TODO - Call method in GoogleDriveManager that:
        *  - uploads the photo to Google Drive;
        *  - adds the photo's URL to user's catalog file
        */
        boolean isSuccess = true;

        if (!isSuccess) {
            Toast.makeText(activity, "Could not add photo", Toast.LENGTH_LONG).show();
            imageView.setImageResource(R.drawable.img_not_available);
            return;
        }

        try {
            MainMenuActivity mainMenuActivity = (MainMenuActivity) activity;
            mainMenuActivity.goToAlbum(albumID, albumName);
        }
        catch (NullPointerException | ClassCastException ex) {
            Toast.makeText(activity, "Could not present new album", Toast.LENGTH_LONG).show();
        }
    }

    public static ArrayList<String> setDropdownAdapterAngGetAlbumIDs(Activity activity, Spinner dropdownMenu) {
        Map<String, String> map = ViewUserAlbumsFragment.getUserMemberships(activity);
        ArrayList<String> albumNames = new ArrayList<>();
        ArrayList<String> albumIDs = new ArrayList<>();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            albumIDs.add(entry.getKey());
            albumNames.add(entry.getValue());
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(activity, android.R.layout.simple_spinner_dropdown_item, albumNames);
        dropdownMenu.setAdapter(adapter);
        return albumIDs;
    }
}
