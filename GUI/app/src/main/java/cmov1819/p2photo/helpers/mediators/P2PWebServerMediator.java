package cmov1819.p2photo.helpers.mediators;

import android.app.Activity;
import android.os.AsyncTask;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

import cmov1819.p2photo.dataobjects.PostRequestData;
import cmov1819.p2photo.dataobjects.PutRequestData;
import cmov1819.p2photo.dataobjects.RequestData;
import cmov1819.p2photo.dataobjects.ResponseData;
import cmov1819.p2photo.helpers.managers.LogManager;
import cmov1819.p2photo.msgtypes.BasicResponse;
import cmov1819.p2photo.msgtypes.ErrorResponse;
import cmov1819.p2photo.msgtypes.SuccessResponse;

import static cmov1819.p2photo.helpers.managers.SessionManager.getSessionID;
import static cmov1819.p2photo.helpers.managers.SessionManager.updateSessionID;

public class P2PWebServerMediator extends AsyncTask<RequestData, Void, ResponseData> {
    private static final String COOKIES_HEADER = "Set-Cookie";

    @Override
    protected ResponseData doInBackground(RequestData... requestDataArray) {
        RequestData requestData = requestDataArray[0];
        LogManager.logSentMessage(requestData);

        Activity activity = requestData.getActivity();
        ResponseData result = new ResponseData(-1, null);
        try {
            URL url = new URL(requestData.getUrl());
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("User-Agent", "P2Photo-App-V0.1");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setConnectTimeout(2000);
            RequestData.RequestType type = requestData.getRequestType();
            switch (type) {
                case SIGNUP:
                    connection.setRequestMethod("POST");
                    result = signup(connection, requestData);
                    break;
                case LOGIN:
                    connection.setRequestMethod("POST");
                    result = login(activity, connection, requestData);
                    break;
                case LOGOUT:
                    result = performDelete(activity, connection);
                    break;
                case SEARCH_USERS:
                    result = performGET(activity, connection);
                    break;
                case GET_CATALOG_TITLE:
                    result = performGET(activity, connection);
                    break;
                case GET_CATALOG:
                    result = performGET(activity, connection);
                    break;
                case NEW_CATALOG:
                    connection.setRequestMethod("POST");
                    result = newCatalog(activity, connection, requestData);
                    break;
                case NEW_CATALOG_SLICE:
                    connection.setRequestMethod("PUT");
                    result = newCatalogSliceFileId(activity, connection, requestData);
                    break;
                case NEW_CATALOG_MEMBER:
                    connection.setRequestMethod("POST");
                    result = newCatalogMember(activity, connection, requestData);
                    break;
                case GET_MEMBERSHIPS:
                    result = performGET(activity, connection);
                    break;
                case GET_GOOGLE_IDENTIFIERS:
                    result = performGET(activity, connection);
                    break;
                case GET_MEMBERSHIP_CATALOG_IDS:
                    result = performGET(activity, connection);
                    break;
                case GET_SERVER_LOGS:
                    result = performSimpleGET(connection);
                    break;
                default:
                    String msg = "Should never be here.";
                    LogManager.logError(LogManager.QUERY_MANAGER_TAG, msg);
                    break;
            }
            connection.disconnect();
            return result;
        }
        catch (IOException ex) {
            LogManager.logError("Query Manager", ex.getMessage());
            return result;
        }
    }

    private void sendJSON(HttpURLConnection connection, JSONObject json) throws IOException {
        OutputStream os = connection.getOutputStream();
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
        writer.flush();
        writer.write(json.toString());
        writer.flush();
    }

    private static boolean is400Response(HttpURLConnection connection) throws IOException {
        return (connection.getResponseCode() >= HttpURLConnection.HTTP_BAD_REQUEST &&
                connection.getResponseCode() <= HttpURLConnection.HTTP_SERVER_ERROR);
    }

    private static InputStreamReader getBufferedReaderFromHttpURLConnection(
            HttpURLConnection connection, boolean isBadRequest) throws IOException {

        if (isBadRequest) {
            return new InputStreamReader(connection.getErrorStream());
        }
        return new InputStreamReader(connection.getInputStream());
    }

    private static String getJSONStringFromHttpResponse(HttpURLConnection connection, boolean isBadRequest)
            throws IOException {

        String currentLine;
        StringBuilder jsonResponse = new StringBuilder();
        InputStreamReader inputStream = getBufferedReaderFromHttpURLConnection(connection, isBadRequest);
        BufferedReader bufferedReader = new BufferedReader(inputStream);
        while ((currentLine = bufferedReader.readLine()) != null) {
            jsonResponse.append(currentLine);
        }
        bufferedReader.close();
        inputStream.close();
        return jsonResponse.toString();
    }

    private static BasicResponse getSuccessResponse(HttpURLConnection connection) throws IOException {
        boolean is400Response = is400Response(connection);
        String jsonResponse = getJSONStringFromHttpResponse(connection, is400Response);
        ObjectMapper objectMapper = new ObjectMapper();
        if (is400Response) {
            return objectMapper.readValue(jsonResponse, ErrorResponse.class);
        }
        return objectMapper.readValue(jsonResponse, SuccessResponse.class);
    }

    private static BasicResponse getBasicResponse(HttpURLConnection connection) throws IOException {
        boolean is400Response = is400Response(connection);
        String jsonResponse = getJSONStringFromHttpResponse(connection, is400Response);
        ObjectMapper objectMapper = new ObjectMapper();
        if (is400Response) {
            return objectMapper.readValue(jsonResponse, ErrorResponse.class);
        }
        return objectMapper.readValue(jsonResponse, BasicResponse.class);
    }

    private void getCookies(Activity activity, HttpURLConnection connection) {
        Map<String, List<String>> headerFields = connection.getHeaderFields();
        List<String> cookiesHeader = headerFields.get(COOKIES_HEADER);
        if (cookiesHeader != null && cookiesHeader.size() > 0) {
            String cookie = cookiesHeader.get(0);
            updateSessionID(activity, cookie);
            String msg = "Received login cookie - " + cookie + ".";
            LogManager.logInfo(LogManager.QUERY_MANAGER_TAG, msg);
        }
        else {
            updateSessionID(activity, "INVALID_SESSION");
            String msg = "No cookies were received.";
            LogManager.logError(LogManager.QUERY_MANAGER_TAG, msg);
        }
    }

    private ResponseData signup(HttpURLConnection connection, RequestData requestData) throws IOException {
        PostRequestData postData = (PostRequestData) requestData;
        sendJSON(connection, postData.getParams());
        BasicResponse payload = getBasicResponse(connection);
        return new ResponseData(connection.getResponseCode(), payload);
    }

    private ResponseData login(Activity activity, HttpURLConnection connection,
                               RequestData requestData) throws IOException {

        PostRequestData postData = (PostRequestData) requestData;
        sendJSON(connection, postData.getParams());
        getCookies(activity, connection);
        BasicResponse payload = getSuccessResponse(connection);
        return new ResponseData(connection.getResponseCode(), payload);
    }

    private ResponseData performDelete(Activity activity, HttpURLConnection connection) throws IOException {
        connection.setRequestProperty("Cookie", "sessionId=" + getSessionID(activity));
        connection.setRequestMethod("DELETE");
        connection.connect();
        BasicResponse payload = P2PWebServerMediator.getBasicResponse(connection);
        return new ResponseData(connection.getResponseCode(), payload);
    }

    private ResponseData newCatalog(Activity activity,
                                    HttpURLConnection connection,
                                    RequestData requestData) throws IOException {

        connection.setRequestProperty("Cookie", "sessionId=" + getSessionID(activity));
        PostRequestData postData = (PostRequestData) requestData;
        sendJSON(connection, postData.getParams());
        connection.connect();

        BasicResponse payload = P2PWebServerMediator.getSuccessResponse(connection);
        return new ResponseData(connection.getResponseCode(), payload);
    }

    private ResponseData newCatalogSliceFileId(Activity activity,
                                               HttpURLConnection connection,
                                               RequestData requestData) throws IOException {

        connection.setRequestProperty("Cookie", "sessionId=" + getSessionID(activity));
        PutRequestData putData = (PutRequestData) requestData;
        sendJSON(connection, putData.getParams());
        connection.connect();
        BasicResponse payload = P2PWebServerMediator.getSuccessResponse(connection);
        return new ResponseData(connection.getResponseCode(), payload);
    }

    private ResponseData newCatalogMember(Activity activity, HttpURLConnection connection,
                                          RequestData requestData) throws IOException {

        connection.setRequestProperty("Cookie", "sessionId=" + getSessionID(activity));
        PostRequestData postData = (PostRequestData) requestData;
        sendJSON(connection, postData.getParams());
        connection.connect();
        BasicResponse payload = P2PWebServerMediator.getBasicResponse(connection);
        return new ResponseData(connection.getResponseCode(), payload);
    }

    private ResponseData performGET(Activity activity, HttpURLConnection connection) throws IOException {
        connection.setRequestProperty("Cookie", "sessionId=" + getSessionID(activity));
        return performSimpleGET(connection);
    }

    private ResponseData performSimpleGET(HttpURLConnection connection) throws IOException {
        connection.setRequestMethod("GET");
        connection.setDoOutput(false);
        connection.connect();
        BasicResponse payload = getSuccessResponse(connection);
        return new ResponseData(connection.getResponseCode(), payload);
    }
}
