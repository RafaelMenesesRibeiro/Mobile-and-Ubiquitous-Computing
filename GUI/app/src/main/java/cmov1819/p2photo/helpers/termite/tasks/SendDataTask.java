package cmov1819.p2photo.helpers.termite.tasks;

import android.os.AsyncTask;

import org.json.JSONObject;

import java.io.IOException;
import java.net.UnknownHostException;

import pt.inesc.termite.wifidirect.SimWifiP2pDevice;
import pt.inesc.termite.wifidirect.sockets.SimWifiP2pSocket;

import static cmov1819.p2photo.helpers.managers.LogManager.SEND_DATA_TASK_TAG;
import static cmov1819.p2photo.helpers.managers.LogManager.logError;
import static cmov1819.p2photo.helpers.managers.LogManager.logInfo;
import static cmov1819.p2photo.helpers.managers.LogManager.logWarning;
import static cmov1819.p2photo.helpers.termite.Consts.waitAndTerminate;
import static cmov1819.p2photo.helpers.termite.tasks.WifiDirectUtils.doSend;
import static cmov1819.p2photo.helpers.termite.tasks.WifiDirectUtils.receiveResponse;

public class SendDataTask extends AsyncTask<Object, String, Void> {
    private static final int TERMITE_PORT = 10001;

    @Override
    protected void onPreExecute() {
        logInfo(SEND_DATA_TASK_TAG, "Started new SendData task...");
    }

    @Override
    protected Void doInBackground(final Object... params) {
        SimWifiP2pDevice targetDevice = (SimWifiP2pDevice) params[1];
        JSONObject jsonData = (JSONObject) params[2];

        SimWifiP2pSocket clientSocket = null;
        try {
            logInfo(SEND_DATA_TASK_TAG, "Creating client socket...");
            clientSocket = new SimWifiP2pSocket(targetDevice.getVirtIp(), TERMITE_PORT);
            doSend(SEND_DATA_TASK_TAG, clientSocket, jsonData);
            receiveResponse(SEND_DATA_TASK_TAG, clientSocket);
        } catch (UnknownHostException uhe) {
            logWarning(SEND_DATA_TASK_TAG,"Specified target device is unreachable, host does not exist!");
        } catch (IOException ioe) {
            logError(SEND_DATA_TASK_TAG,"Failed to open client socket!");
        } finally {
            waitAndTerminate(5000, clientSocket);
        }

        return null;
    }

    @Override
    protected void onPostExecute(Void result) {
        logInfo(SEND_DATA_TASK_TAG, "SendData task finished successfully...");
    }
}