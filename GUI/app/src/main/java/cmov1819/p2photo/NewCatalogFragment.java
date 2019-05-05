package cmov1819.p2photo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.util.concurrent.ExecutionException;

import cmov1819.p2photo.dataobjects.PostRequestData;
import cmov1819.p2photo.dataobjects.PutRequestData;
import cmov1819.p2photo.dataobjects.RequestData;
import cmov1819.p2photo.dataobjects.ResponseData;
import cmov1819.p2photo.exceptions.FailedOperationException;
import cmov1819.p2photo.helpers.managers.AuthStateManager;
import cmov1819.p2photo.helpers.managers.LogManager;
import cmov1819.p2photo.helpers.managers.QueryManager;
import cmov1819.p2photo.helpers.mediators.GoogleDriveMediator;
import cmov1819.p2photo.msgtypes.ErrorResponse;
import cmov1819.p2photo.msgtypes.SuccessResponse;

import static android.widget.Toast.LENGTH_LONG;
import static cmov1819.p2photo.helpers.managers.SessionManager.getUsername;

public class NewCatalogFragment extends Fragment {
    private static final String NEW_CATALOG_TAG = "NEW CATALOG FRAGMENT";

    private AuthStateManager authStateManager;
    private GoogleDriveMediator googleDriveMediator;
    private Activity activity;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        activity = getActivity();
        authStateManager = AuthStateManager.getInstance(this.getContext());
        googleDriveMediator = GoogleDriveMediator.getInstance(authStateManager.getAuthState().getAccessToken());
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
            Toast toast = Toast.makeText(this.getContext(), "Enter a name for the album", Toast.LENGTH_LONG);
            toast.show();
            return;
        }

        try {
            String catalogId = newCatalog(catalogTitle);
            LogManager.logNewCatalog(catalogId, catalogTitle);
            MainMenuActivity mainMenuActivity = (MainMenuActivity) activity;
            mainMenuActivity.goToCatalog(catalogId, catalogTitle);
        }
        catch (FailedOperationException foex) {
            Toast.makeText(this.getContext(), "The create catalog operation failed. Try again later", Toast.LENGTH_LONG).show();
        }
        catch (NullPointerException | ClassCastException ex) {
            Toast.makeText(activity, "Could not present new catalog", Toast.LENGTH_LONG).show();
        }
    }

    private String newCatalog(String catalogTitle) {
        String url = getString(R.string.p2photo_host) + getString(R.string.new_catalog);

        try {
            JSONObject requestBody = new JSONObject();
            requestBody.put("catalogTitle", catalogTitle);
            requestBody.put("calleeUsername", getUsername(activity));
            RequestData requestData = new PostRequestData(activity, RequestData.RequestType.NEW_CATALOG, url, requestBody);
            ResponseData result = new QueryManager().execute(requestData).get();

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

            googleDriveMediator.newCatalogSlice(
                    getActivity(),
                    catalogTitle,
                    catalogID,
                    authStateManager.getAuthState()
            );

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

    public static void newCatalogSlice(final Context context,
                                       final String catalogId,
                                       final String parentFolderGoogleId,
                                       final String catalogFileGoogleId,
                                       final String webContentLink) {
        try {

            JSONObject requestBody = new JSONObject();

            requestBody.put("parentFolderGoogleId", parentFolderGoogleId);
            requestBody.put("catalogFileGoogleId", catalogFileGoogleId);
            requestBody.put("webContentLink", webContentLink);
            requestBody.put("calleeUsername", getUsername((Activity)context));

            String url =
                    context.getString(R.string.p2photo_host) + context.getString(R.string.new_catalog_slice) + catalogId;

            RequestData requestData = new PutRequestData(
                    (Activity)context, RequestData.RequestType.NEW_CATALOG_SLICE, url, requestBody
            );

            ResponseData result = new QueryManager().execute(requestData).get();

            int code = result.getServerCode();

            if (code != HttpURLConnection.HTTP_OK) {
                String reason = ((ErrorResponse) result.getPayload()).getReason();
                if (code == HttpURLConnection.HTTP_UNAUTHORIZED) {
                    LogManager.logError(LogManager.NEW_CATALOG_TAG, reason);
                    Toast.makeText(context, "Session timed out, please login again", Toast.LENGTH_SHORT).show();
                    context.startActivity(new Intent(context, LoginActivity.class));
                }
                else {
                    LogManager.logError(LogManager.NEW_CATALOG_TAG, reason);
                    Toast.makeText(context, "Something went wrong", LENGTH_LONG).show();;
                }
            }

        } catch (JSONException | ExecutionException | InterruptedException ex) {
            throw new FailedOperationException(ex.getMessage());
        }
    }
}
