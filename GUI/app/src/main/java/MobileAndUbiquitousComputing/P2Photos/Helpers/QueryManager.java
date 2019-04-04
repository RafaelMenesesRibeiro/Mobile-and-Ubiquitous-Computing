package MobileAndUbiquitousComputing.P2Photos.Helpers;

import android.app.Activity;
import android.os.AsyncTask;
import android.util.Log;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Map;

import MobileAndUbiquitousComputing.P2Photos.DataObjects.PostRequestData;
import MobileAndUbiquitousComputing.P2Photos.DataObjects.RequestData;
import MobileAndUbiquitousComputing.P2Photos.DataObjects.ResponseData;
import MobileAndUbiquitousComputing.P2Photos.DataObjects.UsersResponseData;
import MobileAndUbiquitousComputing.P2Photos.MsgTypes.BasicResponse;
import MobileAndUbiquitousComputing.P2Photos.MsgTypes.SuccessResponse;

import static MobileAndUbiquitousComputing.P2Photos.Helpers.ConnectionManager.P2PHOTO_DOMAIN;
import static MobileAndUbiquitousComputing.P2Photos.Helpers.ConnectionManager.P2PHOTO_HOST;

public class QueryManager extends AsyncTask<RequestData, Void, ResponseData> {
    private static final String COOKIES_HEADER = "Set-Cookie";

    @Override
    protected ResponseData doInBackground(RequestData... requestDataArray) {
        Log.i("STATUS", "Starting request");
        RequestData requestData = requestDataArray[0];
        Log.i("STATUS", "\nPARAMETERS: \n" + requestData.toString());

        Activity activity = requestData.getActivity();
        ResponseData result = new ResponseData(-1, null);
        try {
            URL url = new URL(requestData.getUrl());
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept","application/json");
            connection.setDoOutput(true); // TODO THIS SHOULD BE FALSE IN GET REQUESTS
            connection.setDoInput(true);
            RequestData.RequestType type = requestData.getRequestType();
            switch (type) {
                case SIGNUP:
                    connection.setRequestMethod("POST");
                    result = signup(connection, requestData);
                    break;
                case LOGIN:
                    connection.setRequestMethod("POST");
                    connection.setRequestProperty("User-Agent", "P2Photo-App-V0.1");
                    result = login(activity, connection, requestData);
                    break;
                case LOGOUT:
                    connection.setRequestMethod("DELETE");
                    result = logout(activity, connection);
                    break;
                case FINDUSERS:
                    connection.setRequestMethod("GET");
                    connection.setDoOutput(false);
                    result = findUsers(activity, connection, requestData);
                    break;
                default:
                    Log.i("ERROR", "Should never be here.");
                    break;
            }
            connection.disconnect();
            return result;
        }
        catch (IOException ex) {
            Log.i("ERROR", ex.getMessage());
            ex.printStackTrace();
            return result;
        }
    }

    private static SuccessResponse getSuccessResponse(HttpURLConnection connection) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        String jsonResponse = getJSONStringFromHttpResponse(connection);
        return objectMapper.readValue(jsonResponse, SuccessResponse.class);
    }

    private static BasicResponse getBasicResponse(HttpURLConnection connection) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        String jsonResponse = getJSONStringFromHttpResponse(connection);
        return objectMapper.readValue(jsonResponse, BasicResponse.class);
    }

    private static String getJSONStringFromHttpResponse(HttpURLConnection connection) throws IOException {
        String currentLine;
        StringBuilder jsonResponse = new StringBuilder();
        InputStreamReader inputStream = new InputStreamReader(connection.getInputStream());
        BufferedReader bufferedReader = new BufferedReader(inputStream);
        while ((currentLine = bufferedReader.readLine()) != null) {
            jsonResponse.append(currentLine);
        }
        bufferedReader.close();
        inputStream.close();
        return jsonResponse.toString();
    }

    private void sendJSON(HttpURLConnection connection, JSONObject json) throws IOException {
        OutputStream os = connection.getOutputStream();
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
        writer.flush();
        writer.write(json.toString());
        writer.flush();
    }

    private void getCookies(Activity activity, HttpURLConnection connection) {
        Map<String, List<String>> headerFields = connection.getHeaderFields();
        List<String> cookiesHeader = headerFields.get(COOKIES_HEADER);
        if (cookiesHeader != null) {
            for (String cookie : cookiesHeader) {
                // TODO - Check if cookie is in fact SessionID. //
                System.out.println("NEW COOKIE: " + cookie);
                SessionID.updateSessionID(activity, cookie);
            }
        }
    }

    private ResponseData signup(HttpURLConnection connection, RequestData requestData) throws IOException {
        PostRequestData postData = (PostRequestData) requestData;
        sendJSON(connection, postData.getParams());
        BasicResponse payload = QueryManager.getBasicResponse(connection);
        return new ResponseData(payload.getCode(), payload);
    }

    private ResponseData login(Activity activity, HttpURLConnection connection, RequestData requestData) throws IOException {
        PostRequestData postData = (PostRequestData) requestData;
        sendJSON(connection, postData.getParams());
        getCookies(activity, connection);
        SuccessResponse payload = QueryManager.getSuccessResponse(connection);
        return new ResponseData(payload.getCode(), payload);
    }

    private ResponseData logout(Activity activity, HttpURLConnection connection) throws IOException {
        String cookie = "sessionId=" + SessionID.getSessionID(activity);
        connection.setRequestProperty("Cookie", cookie);
        connection.connect();
        BasicResponse payload = QueryManager.getBasicResponse(connection);
        return new ResponseData(payload.getCode(), payload);
    }

    private ResponseData findUsers(Activity activity, HttpURLConnection connection, RequestData requestData) throws IOException {
        String cookie = "sessionId=" + SessionID.getSessionID(activity);
        connection.setRequestProperty("Cookie", cookie);
        connection.connect();
        Log.i("WARNINGININASDA", "Server code: " + connection.getResponseCode());
        Log.i("WARNINGININASDA", "Server code: " + connection.getResponseMessage());
        SuccessResponse payload = QueryManager.getSuccessResponse(connection);
        Object result = payload.getResult();
        System.out.println("GOT AN ANSWER. CLASS OF OBJECT IS: " + result.getClass().getName());
        System.out.println("GOT AN ANSWER. OBJECT: " + result.toString());
        return new ResponseData(payload.getCode(), payload);

        /*
        InputStream is = connection.getInputStream();
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        UsersResponseData result = new UsersResponseData(ResponseData.ResponseCode.NO_CODE, -1);
        List<UserData> usersData = new ArrayList<>();
        String output;
        if ((output = br.readLine()) != null) {
            Log.i("STATUS", "NEW LINE: " + output);
            JSONObject jsonObject = new JSONObject(output);

            int code = (int) jsonObject.get("code");
            if (code == 200) {
                try {
                    HashMap<String, ArrayList<BigDecimal>> usersAlbumMap = (HashMap<String, ArrayList<BigDecimal>>) jsonObject.get("result");
                    for (Object o : usersAlbumMap.entrySet()) {
                        Map.Entry<String, ArrayList<BigDecimal>> pair = (Map.Entry<String, ArrayList<BigDecimal>>) o;
                        UserData user = new UserData(pair.getKey(), pair.getValue());
                        usersData.add(user);
                    }
                    return new UsersResponseData();
                }
                catch (ClassCastException ccex) {
                    Log.i("ERROR", "Failed to cast while trying to find user.");
                    result = new UsersResponseData();
                }
            }
            else {
                result = new UsersResponseData();
            }
        }
        is.close();
        br.close();
        return result;
        */
    }
}
