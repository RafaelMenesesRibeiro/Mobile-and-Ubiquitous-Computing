package cmov1819.p2photo.helpers.termite.tasks;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import cmov1819.p2photo.helpers.termite.Consts;
import pt.inesc.termite.wifidirect.sockets.SimWifiP2pSocket;

import static cmov1819.p2photo.helpers.managers.LogManager.logError;
import static cmov1819.p2photo.helpers.termite.Consts.REFUSED;

public class WifiDirectUtils {

    public static void doSend(String tag, SimWifiP2pSocket clientSocket, JSONObject jsonData) {
        try {
            clientSocket.getOutputStream().write((jsonData.toString() + Consts.SEND).getBytes());
        } catch (IOException ioe) {
            logError(tag,"Could not effectuate write on output socket...");
        }
    }

    public static String receiveResponse(String tag, SimWifiP2pSocket clientSocket) {
        try {
            InputStream inputStream = clientSocket.getInputStream();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            return bufferedReader.readLine();
        } catch (IOException ioe) {
            logError(tag,"Could not read reply from output socket input stream...");
        }
        return REFUSED;
    }
}
