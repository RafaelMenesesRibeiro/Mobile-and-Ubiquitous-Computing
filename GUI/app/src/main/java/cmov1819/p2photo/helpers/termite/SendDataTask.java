package cmov1819.p2photo.helpers.termite;

import android.os.AsyncTask;
import android.util.Log;

import org.mortbay.jetty.Main;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import cmov1819.p2photo.MainMenuActivity;
import pt.inesc.termite.wifidirect.sockets.SimWifiP2pSocket;

public class SendDataTask extends AsyncTask<Object, String, Void> {

    private static final String SENDD_DATA_TASK_TAG = "SEND DATA";

    @Override
    protected void onPreExecute() {
        // TODO if pre processing is required
    }

    @Override
    protected Void doInBackground(final Object... params) {
        MainMenuActivity requestingActivity = (MainMenuActivity) params[0];
        SimWifiP2pSocket raClientSocket = requestingActivity.getClientSocket();
        byte[] data = (byte[]) params[1];
        try {
            raClientSocket.getOutputStream().write(data);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(raClientSocket.getInputStream()));
            bufferedReader.readLine();
            raClientSocket.close(); // TODO this is likely incorrect, we might want to keep the socket open.
        } catch (IOException ioe) {
            Log.e(SENDD_DATA_TASK_TAG, "Error: " + ioe.getMessage());

        }
        return null;
    }

    @Override
    protected void onPostExecute(Void result) {
        // TODO investigate if we want to do something after doInBackground ends
    }
}