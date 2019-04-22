package cmov1819.p2photo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import cmov1819.p2photo.dataobjects.RequestData;
import cmov1819.p2photo.dataobjects.ResponseData;
import cmov1819.p2photo.helpers.managers.QueryManager;
import cmov1819.p2photo.msgtypes.SuccessResponse;

import static cmov1819.p2photo.MainMenuActivity.START_SCREEN;
import static cmov1819.p2photo.ViewAlbumFragment.ALBUM_ID_EXTRA;
import static cmov1819.p2photo.ViewAlbumFragment.ALBUM_TITLE_EXTRA;
import static cmov1819.p2photo.dataobjects.RequestData.RequestType.GET_MEMBERSHIPS;
import static cmov1819.p2photo.helpers.managers.SessionManager.getUsername;

public class ViewUserAlbumsFragment extends Fragment {
    private Activity activity;
    private ArrayList<String> catalogIdList;
    private ArrayList<String> catalogTitleList;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        activity = getActivity();
        View view = inflater.inflate(R.layout.fragment_view_user_albums, container, false);

        ListView userAlbumsListView = view.findViewById(R.id.userAlbumsList);
        this.catalogIdList = new ArrayList<>();
        this.catalogTitleList = new ArrayList<>();
        buildCatalogArrays();
        userAlbumsListView.setAdapter(new ArrayAdapter<>(activity, android.R.layout.simple_list_item_1, catalogTitleList));
        userAlbumsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String catalogID = catalogIdList.get(position);
                String catalogTitle = catalogTitleList.get(position);

                try {
                    MainMenuActivity mainMenuActivity = (MainMenuActivity) activity;
                    mainMenuActivity.goToAlbum(catalogID, catalogTitle);
                }
                catch (NullPointerException | ClassCastException ex) {
                    Toast.makeText(activity, "Could not present album", Toast.LENGTH_LONG).show();
                }
            }
        });
        return view;
    }

    private void buildCatalogArrays() {
        Map<String, String> memberships = getUserMemberships(activity);
        for (Map.Entry<String, String> entry : memberships.entrySet()) {
            catalogIdList.add(entry.getKey());
            catalogTitleList.add(entry.getValue());
        }
    }

    public static Map<String, String> getUserMemberships(Activity activity) {
        String url = activity.getString(R.string.get_memberships_endpoint) + "?calleeUsername=" + getUsername(activity);
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        try {
            RequestData requestData = new RequestData(activity, GET_MEMBERSHIPS, url);
            ResponseData responseData = new QueryManager().execute(requestData).get();
            if (responseData.getServerCode() == HttpURLConnection.HTTP_OK) {
                SuccessResponse payload = (SuccessResponse) responseData.getPayload();
                Object object = payload.getResult();
                map = (LinkedHashMap<String, String>) object;
            }
        }
        catch (ClassCastException | ExecutionException | InterruptedException ex) {
            Thread.currentThread().interrupt();
            Log.i("ERROR", "VIEW USER ALBUMS: " + ex.getMessage());
        }
        return map;
    }
}
