package cmov1819.p2photo;

import android.app.Activity;
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
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

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
    private ArrayList<String> albumNames;
    private ArrayList<String> albumIDs;
    private String catalogID;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_view_album, container, false);
        populate(view);

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
                catalogID = albumIDs.get(index);
                String catalogName = albumNames.get(index);
                populateGrid(view, catalogName, new ArrayList<String>());
            }
        });
        return view;
    }

    private void populate(View view) {
        if (getArguments() == null) {
            Log.i("ERROR", "VIEW ALBUM: arguments passed to fragment are null");
            return;
        }

        catalogID = getArguments().getString("catalogID");
        if (catalogID == null) {
            Log.i("ERROR", "VIEW ALBUM: catalogID is null.");
            return;
        }
        if (catalogID.equals("NO_ALBUM_SELECTED_ERROR")) {
            RelativeLayout relativeLayout = view.findViewById(R.id.albumViewContainer);
            relativeLayout.setVisibility(View.INVISIBLE);
            ConstraintLayout constraintLayout = view.findViewById(R.id.dropdownContainer);
            constraintLayout.setVisibility(View.VISIBLE);

            Spinner membershipDropdown = view.findViewById(R.id.membershipDropdownMenu);
            HashMap<String, String> map = ViewUserAlbumsFragment.getUserMemberships(getActivity());
            albumNames = new ArrayList<>();
            albumIDs = new ArrayList<>();
            for (Map.Entry<String, String> entry : map.entrySet()) {
                albumIDs.add(entry.getKey());
                albumNames.add(entry.getValue());
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, albumNames);
            membershipDropdown.setAdapter(adapter);

            return;
        }

        String catalogTitle = getArguments().getString("title");
        ArrayList<String> slicesURLList = getArguments().getStringArrayList("slices");
        if (catalogTitle == null) {
            Log.i("ERROR", "VIEW ALBUM: catalogTitle is null.");
            return;
        }
        if (slicesURLList == null) {
            Log.i("ERROR", "VIEW ALBUM: slicesURLList is null.");
            return;
        }

        populateGrid(view, catalogTitle, slicesURLList);
    }

    private void populateGrid(View view, String catalogTitle, ArrayList<String> slicesURLList) {
        TextView catalogTitleTextView = view.findViewById(R.id.albumTitleLabel);
        catalogTitleTextView.setText(catalogTitle);

        if (slicesURLList.isEmpty()) {
            Toast.makeText(getContext(), "UPS IM EMPTY", LENGTH_LONG).show();
            return;
        }

        // TODO - @FranciscoBarros //
        /*
        GridView grid = findViewById(R.id.albumGrid);

        grid.setAdapter(new ImageGridAdapter(this, imageIdsArray));
        grid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Toast.makeText(ViewAlbumFragment.this, "IMAGE WAS CLICKED: " + position,
                        Toast.LENGTH_SHORT).show();
            }
        });*/
    }

    private void addUserClicked() {
        Fragment newAlbumMemberFragment = new NewAlbumMemberFragment();
        Bundle newAlbumMemberData = new Bundle();
        newAlbumMemberData.putString("albumID", catalogID);
        newAlbumMemberFragment.setArguments(newAlbumMemberData);

        try {
            Activity activity = getActivity();
            MainMenuActivity mainMenuActivity = (MainMenuActivity) activity;
            mainMenuActivity.changeFragment(newAlbumMemberFragment, R.id.nav_new_album_member);
        }
        catch (NullPointerException | ClassCastException ex) {
            Toast.makeText(getContext(), "Could not present add new member screen", Toast.LENGTH_LONG).show();
        }
    }

    private void addPhotoClicked() {
        Fragment addPhotoFragment = new AddPhotosFragment();
        Bundle addPhotoData = new Bundle();
        addPhotoData.putString("albumID", catalogID);
        addPhotoFragment.setArguments(addPhotoData);

        try {
            Activity activity = getActivity();
            MainMenuActivity mainMenuActivity = (MainMenuActivity) activity;
            mainMenuActivity.changeFragment(addPhotoFragment, R.id.nav_add_photos);
        }
        catch (NullPointerException | ClassCastException ex) {
            Toast.makeText(getContext(), "Could not present add new photo screen", Toast.LENGTH_LONG).show();
        }
    }
}
