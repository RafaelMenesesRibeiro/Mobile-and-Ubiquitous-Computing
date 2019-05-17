package cmov1819.p2photo.helpers.termite.tasks;

import android.os.AsyncTask;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.UnknownHostException;

import cmov1819.p2photo.helpers.managers.LogManager;
import cmov1819.p2photo.helpers.managers.WifiDirectManager;
import pt.inesc.termite.wifidirect.SimWifiP2pDevice;
import pt.inesc.termite.wifidirect.sockets.SimWifiP2pSocket;

import static cmov1819.p2photo.helpers.managers.LogManager.logError;
import static cmov1819.p2photo.helpers.managers.LogManager.logWarning;

public class SendDataTask extends AsyncTask<Object, String, Void> {
    private static final String SEND_DATA_TASK_TAG = "SEND DATA";
    private static final String CONFIRM_RCV = "\n";
    private static final String SEND = "\n";

    private static final int TERMITE_PORT = 10001;

    private WifiDirectManager wiFiDirectManager = null;

    @Override
    protected void onPreExecute() {
        LogManager.logInfo(SEND_DATA_TASK_TAG, "Started new SendData task...");
    }

    @Override
    protected Void doInBackground(final Object... params) {
        // get singleton instance of our WifiDirectManager
        wiFiDirectManager = WifiDirectManager.getInstance();
        SimWifiP2pDevice targetDevice = (SimWifiP2pDevice) params[1];
        JSONObject jsonData = (JSONObject) params[2];

        try {
            LogManager.logInfo(SEND_DATA_TASK_TAG, "Creating client socket...");
            SimWifiP2pSocket clientSocket = new SimWifiP2pSocket(targetDevice.getVirtIp(), TERMITE_PORT);
            doSend(clientSocket, jsonData);
            receiveResponse(clientSocket);
            try {
                clientSocket.close();
                LogManager.logInfo(SEND_DATA_TASK_TAG, "Closed client socket. Operation completed...");
            } catch (IOException e) {
                logError(SEND_DATA_TASK_TAG,"Error closing client socket!");
            }
        } catch (UnknownHostException uhe) {
            logWarning(SEND_DATA_TASK_TAG,"Specified target device is unreachable, host does not exist!");
        } catch (IOException ioe) {
            logError(SEND_DATA_TASK_TAG,"Failed to open client socket!");
        }

        return null;
    }

    private void receiveResponse(SimWifiP2pSocket clientSocket) {
        try {
            InputStream inputStream = clientSocket.getInputStream();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            bufferedReader.readLine();
        } catch (IOException ioe) {
            logError(SEND_DATA_TASK_TAG,"Could not read reply from output socket input stream...");
        }
    }

    private void doSend(SimWifiP2pSocket clientSocket, JSONObject jsonData) {
        try {
            clientSocket.getOutputStream().write((jsonData.toString() + SEND).getBytes());
        } catch (IOException ioe) {
            logError(SEND_DATA_TASK_TAG,"Could not effectuate write on output socket...");
        }
    }

    @Override
    protected void onPostExecute(Void result) {
        LogManager.logInfo(SEND_DATA_TASK_TAG, "SendData task finished successfully...");
    }
}