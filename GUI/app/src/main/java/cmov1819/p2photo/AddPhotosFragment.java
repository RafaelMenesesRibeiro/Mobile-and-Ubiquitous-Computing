package cmov1819.p2photo;

import android.app.Activity;
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

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Map;

public class AddPhotosFragment extends Fragment {
    public static final String ALBUM_ID_EXTRA = "albumID";
    private Activity activity;
    private View view;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        this.activity = getActivity();
        final View inflaterView = inflater.inflate(R.layout.fragment_add_photos, container, false);
        view = inflaterView;

        Button chooseButton = inflaterView.findViewById(R.id.choosePhoto);
        chooseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                choosePhotoClicked();
            }
        });
        Button done = inflaterView.findViewById(R.id.done);
        done.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addPhotosClicked(inflaterView);
            }
        });
        populate(inflaterView);
        return inflaterView;
    }

    private void populate(View view) {
        ImageView imageView = view.findViewById(R.id.imageView);
        imageView.setImageResource(R.drawable.img_not_available);

        Spinner membershipDropdown = view.findViewById(R.id.membershipDropdownMenu);
        Map<String, String> map = ViewUserAlbumsFragment.getUserMemberships(activity);
        ArrayList<String> albumNames = new ArrayList<>();
        ArrayList<String> albumIDs = new ArrayList<>();
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
            }
            catch (FileNotFoundException | NullPointerException ex) {
                imageView.setImageResource(R.drawable.img_not_available);
                Log.i("ERROR", "AddPhotos: Could not load selected image file " + targetUri);
            }
        }
    }

    public void addPhotosClicked(View view) {
        // TODO - @FranciscoBarros //
    }
}
