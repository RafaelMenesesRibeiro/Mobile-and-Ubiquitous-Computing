package MobileAndUbiquitousComputing.P2Photos.Helpers;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import MobileAndUbiquitousComputing.P2Photos.DataObjects.PostRequestData;
import MobileAndUbiquitousComputing.P2Photos.DataObjects.RequestData;
import MobileAndUbiquitousComputing.P2Photos.DataObjects.ResponseData;
import MobileAndUbiquitousComputing.P2Photos.DataObjects.UserData;
import MobileAndUbiquitousComputing.P2Photos.DataObjects.UsersResponseData;

public class QueryManager extends AsyncTask<RequestData, Void, ResponseData> {

    @Override
    protected ResponseData doInBackground(RequestData... requestData) {
        Log.i("STATUS", "Starting request");
        RequestData rData = requestData[0];
        Log.i("STATUS", "\nPARAMETERS: \n" + rData.toString());

        ResponseData result = new ResponseData(ResponseData.ResponseCode.UNSUCCESS, -1);
        try {
            URL url = new URL(rData.getUrl()); // TODO rData is not a human readable name, requestData as suggested by
            // IDE
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true); // TODO THIS SHOULD BE FALSE IN GET REQUESTS
            connection.setDoInput(true);

            RequestData.RequestType type = rData.getRequestType();
            switch (type) {
                case GETFINDUSER:
                    connection.setRequestMethod("GET");
                    result = FindUser(connection, rData);
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
                case PUT:
                    break;
                case DELETE:
                    connection.setRequestMethod("DELETE");
                    result = DeleteRequest(connection, rData);
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
        catch (JSONException jex) {
            jex.printStackTrace();
            return result;
        }
    }

    private ResponseData PostRequest(HttpURLConnection connection, RequestData requestData) throws IOException, JSONException {
        PostRequestData postData = (PostRequestData) requestData;
        JSONObject json = postData.getParams();

        OutputStream os = connection.getOutputStream();
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
        writer.flush();
        writer.write(json.toString());
        writer.flush();

        ResponseData result = GetResponseData(connection);
        return result;
    }

    private ResponseData DeleteRequest(HttpURLConnection connection, RequestData requestData) throws IOException, JSONException {
        connection.connect();
        ResponseData result = GetResponseData(connection);
        return result;
    }

    private ResponseData GetResponseData(HttpURLConnection connection) throws IOException, JSONException {
        InputStream is = connection.getInputStream();
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        ResponseData result = new ResponseData(ResponseData.ResponseCode.NO_CODE, -1);
        String output;
        if ((output = br.readLine()) != null) {
            Log.i("STATUS", "NEW LINE: " + output);
            JSONObject jsonObject = new JSONObject(output);

            int code = (int) jsonObject.get("code");
            if (code == 200) {
                result = new ResponseData(ResponseData.ResponseCode.SUCCESS, 200);
            }
            else {
                result = new ResponseData(ResponseData.ResponseCode.UNSUCCESS, code);
            }
        }
        is.close();
        br.close();
        return result;
    }

    private UsersResponseData FindUser(HttpURLConnection connection, RequestData requestData) throws IOException, JSONException {
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
                    return new UsersResponseData(ResponseData.ResponseCode.SUCCESS, code, usersData);
                }
                catch (ClassCastException ccex) {
                    Log.i("ERROR", "Failed to cast while trying to find user.");
                    result = new UsersResponseData(ResponseData.ResponseCode.UNSUCCESS, code);
                }
            }
            else {
                result = new UsersResponseData(ResponseData.ResponseCode.UNSUCCESS, code);
            }
        }
        is.close();
        br.close();
        return result;
    }
}
