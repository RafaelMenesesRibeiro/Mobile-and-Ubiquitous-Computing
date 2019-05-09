package cmov1819.p2photo.helpers.termite;

import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import cmov1819.p2photo.R;
import pt.inesc.termite.wifidirect.SimWifiP2pDevice;
import pt.inesc.termite.wifidirect.sockets.SimWifiP2pSocket;

public class SendDataTask extends AsyncTask<Object, String, Void> {

    private static final String SEND_DATA_TASK_TAG = "SEND DATA";

    @Override
    protected void onPreExecute() {
        // TODO if pre processing is required
    }

    @Override
    protected Void doInBackground(final Object... params) {
        P2PhotoWiFiDirectManager wiFiDirectManager = (P2PhotoWiFiDirectManager) params[0];

        SimWifiP2pDevice targetDevice = (SimWifiP2pDevice) params[1];
        byte[] data = (byte[]) params[2];
        
        try {
            // Construct a new clientSocket
            SimWifiP2pSocket clientSocket = new SimWifiP2pSocket(targetDevice.getVirtIp(), R.string.termite_port);
            // Send data to target device
            clientSocket.getOutputStream().write(data);
            // Read any reply from target device
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            bufferedReader.readLine();
            clientSocket.close();
        } catch (IOException ioe) {
            Log.e(SEND_DATA_TASK_TAG, "Error: " + ioe.getMessage());
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void result) {
        // TODO investigate if we want to do something after doInBackground ends
    }
}