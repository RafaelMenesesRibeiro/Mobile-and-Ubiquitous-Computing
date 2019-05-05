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
import android.widget.Toast;

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
import cmov1819.p2photo.helpers.managers.QueryManager;
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
            Toast.makeText(getContext(), "The username cannot by empty", Toast.LENGTH_LONG).show();
            return;
        }

        try {
            addMember(catalogID, username);
            LogManager.logNewCatalogMember(catalogID, catalogTitle, username);
            MainMenuActivity mainMenuActivity = (MainMenuActivity) activity;
            mainMenuActivity.goToCatalog(catalogID, catalogTitle);
        }
        catch (NullPointerException | ClassCastException ex) {
            Toast.makeText(activity, "Could not present new album", Toast.LENGTH_LONG).show();
        }
        catch (FailedOperationException foex) {
            Toast.makeText(activity, "The add user to album operation failed. Try again later", Toast.LENGTH_LONG).show();
        }
        catch (NoMembershipException nmex) {
            Toast.makeText(activity, "The add user to album operation failed. No membership", Toast.LENGTH_LONG).show();
        }
        catch (UsernameException uex) {
            Toast.makeText(activity, "The add user to album operation failed. User does not exist", Toast.LENGTH_LONG).show();
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

            ResponseData result = new QueryManager().execute(requestData).get();
            int code = result.getServerCode();

            if (code == HttpURLConnection.HTTP_OK) {
                // TODO - Change screen? //
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
        catch (JSONException | ExecutionException | InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new FailedOperationException(ex.getMessage());
        }
    }
}
