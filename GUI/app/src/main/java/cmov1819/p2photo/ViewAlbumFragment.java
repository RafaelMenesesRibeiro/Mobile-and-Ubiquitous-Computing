package cmov1819.p2photo;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import static android.widget.Toast.LENGTH_LONG;
import static android.widget.Toast.LENGTH_SHORT;

public class ViewAlbumFragment extends Fragment {
    private final Integer imageIdsArray[] = {
            R.drawable.img1,
            R.drawable.img2,
            R.drawable.img3,
            R.drawable.img4,
            R.drawable.img5,
            R.drawable.img6,
            R.drawable.img7,
            R.drawable.img8,
            R.drawable.img9,
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_view_album, container, false);
        try {
            populate(view);
        }
        catch (NullPointerException ex) {
            // TODO - Decide. //
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
        return view;
    }

    private void populate(View view) throws NullPointerException {
        // TODO - @FranciscoBarros //
        String catalogTitle = getArguments().getString("title");
        ArrayList<String> slicesURLList = getArguments().getStringArrayList("slices");

        if (catalogTitle.equals("NO_ALBUM_SELECTED_ERROR?")) {
            // TODO - Implement. //
            Toast.makeText(getContext(), "NO ALBUM SELECTED", LENGTH_SHORT).show();
        }

        TextView catalogTitleTextView = view.findViewById(R.id.albumTitleLabel);
        catalogTitleTextView.setText(catalogTitle);

        if (slicesURLList.isEmpty()) {
            Toast.makeText(getContext(), "UPS IM EMPTY", LENGTH_LONG).show();
        }

        /*
        GridView grid = findViewById(R.id.albumGrid);

        grid.setAdapter(new ImageGridAdapter(this, imageIdsArray));
        grid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // TODO - Implement this. //
                Toast.makeText(ViewAlbumFragment.this, "IMAGE WAS CLICKED: " + position,
                        Toast.LENGTH_SHORT).show();
            }
        });*/
    }

    private void addUserClicked() {
        // TODO - Implement. //
    }

    private void addPhotoClicked() {
        // TODO - Implement. //
    }
}