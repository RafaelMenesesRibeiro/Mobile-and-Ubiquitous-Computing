package cmov1819.p2photo;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

import cmov1819.p2photo.dataobjects.PostRequestData;
import cmov1819.p2photo.dataobjects.RequestData;
import cmov1819.p2photo.dataobjects.ResponseData;
import cmov1819.p2photo.exceptions.FailedOperationException;
import cmov1819.p2photo.exceptions.NoMembershipException;
import cmov1819.p2photo.exceptions.UsernameException;
import cmov1819.p2photo.helpers.managers.LogManager;
import cmov1819.p2photo.helpers.mediators.P2PWebServerMediator;
import cmov1819.p2photo.helpers.managers.SessionManager;
import cmov1819.p2photo.msgtypes.ErrorResponse;

public class NewCatalogMemberFragment extends Fragment {
    public static final String CATALOG_ID_EXTRA = "catalogID";
    private ArrayList<String> catalogIDs;
    private Activity activity;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        activity = getActivity();
        final View view = inflater.inflate(R.layout.fragment_new_catalog_member, container, false);
        populate(view);
        return view;
    }

    private void populate(final View view) {
        Button doneButton = view.findViewById(R.id.done);
        doneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addUserClicked(view);
            }
        });
        EditText editText = view.findViewById(R.id.toAddUsernameInputBox);
        MainMenuActivity.bingEditTextWithButton(editText, doneButton);

        Spinner membershipDropdown = view.findViewById(R.id.membershipDropdownMenu);
        catalogIDs = AddPhotosFragment.setDropdownAdapterAngGetCatalogIDs(activity, membershipDropdown);

        String catalogID;
        if (getArguments() != null && (catalogID = getArguments().getString(CATALOG_ID_EXTRA)) != null) {
            int index = catalogIDs.indexOf(catalogID);
            if (index != -1) {
                membershipDropdown.setSelection(index);
            }
        }
    }

    public void addUserClicked(View view) {
        Spinner catalogIDInput = view.findViewById(R.id.membershipDropdownMenu);
        String catalogID = catalogIDs.get(catalogIDInput.getSelectedItemPosition());
        String catalogTitle = catalogIDInput.getSelectedItem().toString();
        EditText usernameInput = view.findViewById(R.id.toAddUsernameInputBox);
        String username = usernameInput.getText().toString();

        if (username.equals("")) {
            LogManager.toast(activity, "The username cannot by empty");
            return;
        }

        try {
            addMember(catalogID, username);
            LogManager.logNewCatalogMember(catalogID, catalogTitle, username);
            MainMenuActivity mainMenuActivity = (MainMenuActivity) activity;
            mainMenuActivity.goToCatalog(catalogID, catalogTitle);
        }
        catch (NullPointerException | ClassCastException ex) {
            LogManager.toast(activity, "Could not present new album");
        }
        catch (FailedOperationException foex) {
            LogManager.toast(activity, "The add user to album operation failed. Try again later");
        }
        catch (NoMembershipException nmex) {
            LogManager.toast(activity, "The add user to album operation failed. No membership");
        }
        catch (UsernameException uex) {
            LogManager.toast(activity, "The add user to album operation failed. User does not exist");
        }
    }

    private void addMember(String catalogID, String username)
            throws FailedOperationException, NoMembershipException, UsernameException {
        String url = getString(R.string.p2photo_host) + getString(R.string.new_catalog_member);

        try {
            JSONObject requestBody = new JSONObject();
            requestBody.put("catalogId", catalogID);
            requestBody.put("newMemberUsername", username);
            requestBody.put("calleeUsername", SessionManager.getUsername(activity));
            RequestData requestData = new PostRequestData(this.getActivity(),
                    RequestData.RequestType.NEW_CATALOG_MEMBER, url, requestBody);

            ResponseData result = new P2PWebServerMediator().execute(requestData).get();
            int code = result.getServerCode();

            if (code == HttpURLConnection.HTTP_OK) {
                // Do nothing. //
            }
            else if (code == HttpURLConnection.HTTP_BAD_REQUEST) {
                ErrorResponse errorResponse = (ErrorResponse) result.getPayload();
                String msg = errorResponse.getReason();
                LogManager.logError(LogManager.NEW_CATALOG_MEMBER_TAG, msg);
                throw new FailedOperationException(msg);
            }
            else if (code == HttpURLConnection.HTTP_UNAUTHORIZED) {
                ErrorResponse errorResponse = (ErrorResponse) result.getPayload();
                String reason = errorResponse.getReason();
                if (reason.equals(getString(R.string.no_membership))) {
                    String msg = "Callee does not belong to album " + catalogID;
                    LogManager.logError(LogManager.NEW_CATALOG_MEMBER_TAG, msg);
                    throw new NoMembershipException(reason);
                }
            }
            else if (code == HttpURLConnection.HTTP_NOT_FOUND) {
                ErrorResponse errorResponse = (ErrorResponse) result.getPayload();
                String reason = errorResponse.getReason();
                if (reason.equals(getString(R.string.no_user))) {
                    String msg = "Username does not exist " + username;
                    LogManager.logError(LogManager.NEW_CATALOG_MEMBER_TAG, msg);
                    throw new UsernameException(reason);
                }
            }
            else {
                String msg = "Server response code: " + code;
                LogManager.logError(LogManager.NEW_CATALOG_MEMBER_TAG, msg);
                throw new FailedOperationException();
            }
        }
        catch (JSONException ex) {
            String msg = "JSONException: " + ex.getMessage();
            LogManager.logError(LogManager.NEW_CATALOG_MEMBER_TAG, msg);
            throw new FailedOperationException(ex.getMessage());
        }
        catch (ExecutionException | InterruptedException ex) {
            Thread.currentThread().interrupt();
            String msg = "New Catalog Member unsuccessful. " + ex.getMessage();
            LogManager.logError(LogManager.NEW_CATALOG_MEMBER_TAG, msg);
            throw new FailedOperationException(ex.getMessage());
        }
    }
}
