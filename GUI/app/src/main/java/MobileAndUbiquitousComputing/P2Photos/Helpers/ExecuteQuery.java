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
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import MobileAndUbiquitousComputing.P2Photos.DataObjects.PostRequestData;
import MobileAndUbiquitousComputing.P2Photos.DataObjects.RequestData;
import MobileAndUbiquitousComputing.P2Photos.DataObjects.ResponseData;

public class ExecuteQuery extends AsyncTask<RequestData, Void, ResponseData> {

    @Override
    protected ResponseData doInBackground(RequestData... requestData) {
        Log.i("STATUS", "Starting request");
        RequestData rData = requestData[0];
        Log.i("STATUS", "\nPARAMETERS: \n" + rData.toString());

        ResponseData result = new ResponseData(ResponseData.ResponseCode.UNSUCCESS, -1);
        try {
            URL url = new URL(rData.getUrl());
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setDoInput(true);

            RequestData.RequestType type = rData.getRequestType();
            switch (type) {
                case GET:
                    break;
                case POST:
                    connection.setRequestMethod("POST");
                    connection.setRequestProperty("Content-Type", "application/json");
                    connection.setRequestProperty("Accept","application/json");
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
        writer.write(json.toString());
        writer.flush();
        writer.close();
        os.close();

        connection.connect();
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
}
