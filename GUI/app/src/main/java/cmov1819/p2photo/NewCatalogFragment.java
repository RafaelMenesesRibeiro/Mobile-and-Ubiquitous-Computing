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

import org.json.JSONException;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.util.concurrent.ExecutionException;

import cmov1819.p2photo.dataobjects.PostRequestData;
import cmov1819.p2photo.dataobjects.RequestData;
import cmov1819.p2photo.dataobjects.ResponseData;
import cmov1819.p2photo.exceptions.FailedOperationException;
import cmov1819.p2photo.helpers.managers.ArchitectureManager;
import cmov1819.p2photo.helpers.managers.LogManager;
import cmov1819.p2photo.helpers.mediators.P2PWebServerMediator;
import cmov1819.p2photo.msgtypes.ErrorResponse;
import cmov1819.p2photo.msgtypes.SuccessResponse;

import static cmov1819.p2photo.helpers.managers.SessionManager.getUsername;

public class NewCatalogFragment extends Fragment {
    private Activity activity;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        activity = getActivity();
        final View view = inflater.inflate(R.layout.fragment_new_catalog, container, false);
        populate(view);
        return view;
    }

    private void populate(final View view) {
        Button doneButton = view.findViewById(R.id.done);
        doneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                newCatalogClicked(view);
            }
        });
        EditText editText = view.findViewById(R.id.nameInputBox);
        MainMenuActivity.bingEditTextWithButton(editText, doneButton);
    }

    public void newCatalogClicked(View view) {
        EditText titleInput = view.findViewById(R.id.nameInputBox);
        String catalogTitle = titleInput.getText().toString();

        if (catalogTitle.equals("")) {
            LogManager.toast(activity, "Enter a name for the album");
            return;
        }

        try {
            String catalogId = newCatalog(catalogTitle);
            LogManager.logNewCatalog(catalogId, catalogTitle);
            MainMenuActivity mainMenuActivity = (MainMenuActivity) activity;
            mainMenuActivity.goToCatalog(catalogId, catalogTitle);
        }
        catch (FailedOperationException foex) {
            LogManager.toast(activity, "The create catalog operation failed. Try again later");
        }
        catch (NullPointerException | ClassCastException ex) {
            LogManager.toast(activity, "Could not present new catalog");
        }
    }

    private String newCatalog(String catalogTitle) {
        String url = getString(R.string.p2photo_host) + getString(R.string.new_catalog);

        try {
            JSONObject requestBody = new JSONObject();
            requestBody.put("catalogTitle", catalogTitle);
            requestBody.put("calleeUsername", getUsername(activity));
            RequestData requestData = new PostRequestData(activity, RequestData.RequestType.NEW_CATALOG, url, requestBody);
            ResponseData result = new P2PWebServerMediator().execute(requestData).get();

            String catalogID;
            int code = result.getServerCode();
            if (code == HttpURLConnection.HTTP_OK) {
                SuccessResponse payload = (SuccessResponse) result.getPayload();
                catalogID = (String) payload.getResult();
            }
            else {
                ErrorResponse errorResponse = (ErrorResponse) result.getPayload();
                String msg = "The new catalog operation was unsuccessful. Server response code: " + code + ".\n" + result.getPayload().getMessage() + "\n" + errorResponse.getReason();
                LogManager.logError(LogManager.NEW_CATALOG_TAG, msg);
                throw new FailedOperationException();
            }

            ArchitectureManager.systemArchitecture.newCatalogSlice(activity, catalogID, catalogTitle);
            return catalogID;
        }
        catch (JSONException ex) {
            throw new FailedOperationException(ex.getMessage());
        }
        catch (ExecutionException | InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new FailedOperationException(ex.getMessage());
        }
    }
}
