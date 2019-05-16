package cmov1819.p2photo;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import cmov1819.p2photo.dataobjects.RequestData;
import cmov1819.p2photo.dataobjects.ResponseData;
import cmov1819.p2photo.helpers.managers.LogManager;
import cmov1819.p2photo.helpers.mediators.P2PWebServerMediator;
import cmov1819.p2photo.helpers.managers.SessionManager;
import cmov1819.p2photo.msgtypes.SuccessResponse;

import static cmov1819.p2photo.dataobjects.RequestData.RequestType.GET_MEMBERSHIPS;
import static cmov1819.p2photo.dataobjects.RequestData.RequestType.GET_MEMBERSHIP_CATALOG_IDS;
import static cmov1819.p2photo.helpers.managers.SessionManager.getUsername;

public class ViewUserCatalogsFragment extends Fragment {
    private Activity activity;
    private ArrayList<String> catalogIdList;
    private ArrayList<String> catalogTitleList;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        activity = getActivity();
        View view = inflater.inflate(R.layout.fragment_view_user_catalogs, container, false);
        populate(view);
        LogManager.logViewUserCatalogs(SessionManager.getUsername(activity));
        return view;
    }

    private void populate(View view) {
        catalogIdList = new ArrayList<>();
        catalogTitleList = new ArrayList<>();
        Map<String, String> memberships = getMemberships(activity);
        for (Map.Entry<String, String> entry : memberships.entrySet()) {
            catalogIdList.add(entry.getKey());
            catalogTitleList.add(entry.getValue());
        }

        ListView userCatalogsListView = view.findViewById(R.id.userCatalogsList);
        userCatalogsListView.setAdapter(new ArrayAdapter<>(activity, android.R.layout.simple_list_item_1, catalogTitleList));
        userCatalogsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                try {
                    MainMenuActivity mainMenuActivity = (MainMenuActivity) activity;
                    mainMenuActivity.goToCatalog(catalogIdList.get(position), catalogTitleList.get(position));
                }
                catch (NullPointerException | ClassCastException ex) {
                    LogManager.toast(activity, "Could not present album");
                }
            }
        });
    }

    public static Map<String, String> getMemberships(Activity activity) {
        String url = activity.getString(R.string.p2photo_host) + activity.getString(R.string.get_memberships)
                    + "?calleeUsername=" + getUsername(activity);
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        try {
            RequestData requestData = new RequestData(activity, GET_MEMBERSHIPS, url);
            ResponseData responseData = new P2PWebServerMediator().execute(requestData).get();
            if (responseData.getServerCode() == HttpURLConnection.HTTP_OK) {
                SuccessResponse payload = (SuccessResponse) responseData.getPayload();
                Object object = payload.getResult();
                map = (LinkedHashMap<String, String>) object;
            }
        }
        catch (ClassCastException | ExecutionException | InterruptedException ex) {
            Thread.currentThread().interrupt();
            LogManager.logError(LogManager.VIEW_USER_CATALOGS_TAG, ex.getMessage());
        }
        return map;
    }

    public static Map<String, String> getMembershipGoogleDriveIDs(Activity activity) {
        String url = activity.getString(R.string.p2photo_host) + activity.getString(R.string.get_membership_catalog_ids)
                + "?calleeUsername=" + getUsername(activity);
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        try {
            RequestData requestData = new RequestData(activity, GET_MEMBERSHIP_CATALOG_IDS, url);
            ResponseData responseData = new P2PWebServerMediator().execute(requestData).get();
            if (responseData.getServerCode() == HttpURLConnection.HTTP_OK) {
                SuccessResponse payload = (SuccessResponse) responseData.getPayload();
                Object object = payload.getResult();
                map = (LinkedHashMap<String, String>) object;
            }
        }
        catch (ClassCastException | ExecutionException | InterruptedException ex) {
            Thread.currentThread().interrupt();
            LogManager.logError(LogManager.VIEW_USER_CATALOGS_TAG, ex.getMessage());
        }
        return map;
    }
}
