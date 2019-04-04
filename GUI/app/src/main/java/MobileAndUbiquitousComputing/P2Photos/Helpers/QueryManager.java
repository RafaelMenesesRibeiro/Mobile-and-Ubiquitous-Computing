package MobileAndUbiquitousComputing.P2Photos.Helpers;

import android.app.Activity;
import android.os.AsyncTask;
import android.util.Log;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;

import MobileAndUbiquitousComputing.P2Photos.DataObjects.PostRequestData;
import MobileAndUbiquitousComputing.P2Photos.DataObjects.RequestData;
import MobileAndUbiquitousComputing.P2Photos.DataObjects.ResponseData;
import MobileAndUbiquitousComputing.P2Photos.DataObjects.UsersResponseData;
import MobileAndUbiquitousComputing.P2Photos.MsgTypes.BasicResponse;
import MobileAndUbiquitousComputing.P2Photos.MsgTypes.SuccessResponse;

public class QueryManager extends AsyncTask<RequestData, Void, ResponseData> {
    private static final String COOKIES_HEADER = "Set-Cookie";

    @Override
    protected ResponseData doInBackground(RequestData... requestData) {
        Log.i("STATUS", "Starting request");
        RequestData rData = requestData[0];
        Log.i("STATUS", "\nPARAMETERS: \n" + rData.toString());

        Activity activity = rData.getActivity();
        ResponseData result = new ResponseData(-1, null);
        try {
            URL url = new URL(rData.getUrl()); // TODO rData is not a human readable name, requestData as suggested by
            // IDE
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true); // TODO THIS SHOULD BE FALSE IN GET REQUESTS
            connection.setDoInput(true);
            RequestData.RequestType type = rData.getRequestType();
            switch (type) {
                case LOGIN:
                    connection.setRequestMethod("POST");
                    connection.setRequestProperty("Content-Type", "application/json");
                    connection.setRequestProperty("Accept","application/json");
                    connection.setRequestProperty("User-Agent", "P2Photo-App-V0.1");
                    result = login(activity, connection, rData);
                    break;
                case GETFINDUSER:
                    connection.setRequestMethod("GET");
                    // TODO //
                    // result = FindUser(connection, rData);
                    break;
                case GET:
                    break;
                case POST:
                    connection.setRequestMethod("POST");
                    // TODO all requests regardless of type need content-type, accept and u-agent
                    connection.setRequestProperty("Content-Type", "application/json");
                    connection.setRequestProperty("Accept","application/json");
                    connection.setRequestProperty("User-Agent", "P2Photo-App-V0.1");
                    result = PostRequest(connection, rData);
                    break;
                case PUT:
                    break;
                case DELETE:
                    connection.setRequestMethod("DELETE");
                    result = DeleteRequest(connection);
                    break;
                default:
                    Log.i("ERROR", "Should never be here.");
                    break;
            }
            connection.disconnect();
            return result;
        }
        // TODO If you are only printing stacktrace, just do Exception1 | Exception 2 exc { ... }
        // No point having multiple catch statements for different exceptions with same treatment.
        catch (MalformedURLException murlex) {
            murlex.printStackTrace();
            return result;
        }
        catch (IOException ioex) {
            ioex.printStackTrace();
            return result;
        }
    }

    private ResponseData PostRequest(HttpURLConnection connection, RequestData requestData) throws IOException {
        PostRequestData postData = (PostRequestData) requestData;
        JSONObject json = postData.getParams();

        OutputStream os = connection.getOutputStream();
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
        writer.flush();
        writer.write(json.toString());
        writer.flush();
        BasicResponse payload = QueryManager.getBasicResponse(connection);
        return new ResponseData(payload.getCode(), payload);
    }

    private ResponseData DeleteRequest(HttpURLConnection connection) throws IOException {
        connection.connect();
        BasicResponse payload = QueryManager.getBasicResponse(connection);
        return new ResponseData(payload.getCode(), payload);
    }

    private UsersResponseData FindUser(HttpURLConnection connection, RequestData requestData) throws UnsupportedOperationException {
        // TODO - Needs redoing. //
        throw new UnsupportedOperationException();
        /*
        connection.connect();
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

    private ResponseData login(Activity activity, HttpURLConnection connection, RequestData requestData) throws IOException {
        PostRequestData postData = (PostRequestData) requestData;
        sendJSON(connection, postData.getParams());
        getCookies(activity, connection);
        SuccessResponse payload = QueryManager.getSuccessResponse(connection);
        return new ResponseData(payload.getCode(), payload);
    }
}
