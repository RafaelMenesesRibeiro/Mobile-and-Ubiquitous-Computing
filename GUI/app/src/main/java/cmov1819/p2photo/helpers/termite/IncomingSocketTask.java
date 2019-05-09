package cmov1819.p2photo.helpers.termite;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import cmov1819.p2photo.R;
import pt.inesc.termite.wifidirect.sockets.SimWifiP2pSocket;
import pt.inesc.termite.wifidirect.sockets.SimWifiP2pSocketServer;

public class IncomingSocketTask extends AsyncTask<Object, String, Void> {

    private static final String INCOMING_TASK_TAG = "INCOMING SOCKET";

    @Override
    protected void onPreExecute() {
        // TODO if pre processing is required
    }

    @Override
    protected Void doInBackground(final Object... params) {

        Log.d(INCOMING_TASK_TAG, "Started incoming communication task (" + this.hashCode() + ").");

        try {
            P2PhotoWiFiDirectManager wiFiDirectManager = (P2PhotoWiFiDirectManager) params[0];
            // setup a server socket on MainMenuActivity
            wiFiDirectManager.setServerSocket(new SimWifiP2pSocketServer(R.string.termite_port));
            // set server socket to listen to incoming requests
            while (!Thread.currentThread().isInterrupted()) {
                SimWifiP2pSocket socket = wiFiDirectManager.getServerSocket().accept();
                try {
                    // Read from input stream
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    String incomingJsonData = bufferedReader.readLine();
                    // Process values
                    JSONObject jsonObject = new JSONObject(incomingJsonData);

                    if (jsonObject.has("operation")) {
                        String operation = jsonObject.getString("operation");
                        switch (operation) {
                            case "requestCatalog":
                                break;
                            case "sendCatalog":
                                break;
                            case "requestPhoto":
                                break;
                            case "sendPhoto":
                                break;
                            case "areYouServer":
                                break;
                            case "areYouServerReply":
                                break;
                            default:
                                socket.getOutputStream().write(("warning: 'operation' field invalid\n").getBytes());
                        }
                    } else {
                        socket.getOutputStream().write(("error: 'operation' field not found\n").getBytes());
                    }
                    // Close interaction
                    socket.getOutputStream().write(("\n").getBytes());
                } catch (IOException ioe) {
                    Log.e(INCOMING_TASK_TAG, "Error reading socket: " + ioe.getMessage());
                } catch (JSONException jsone) {
                    Log.e(INCOMING_TASK_TAG, "Error reading socket: malformed json request");

                } finally {
                    socket.close();
                }
            }
        } catch (IOException ioe) {
            Log.e(INCOMING_TASK_TAG, "Socket error: " + ioe.getMessage());
        }

        return null;
    }

    @Override
    protected void onPostExecute(Void result) {
        // TODO investigate if we want to do something after doInBackground ends
    }

}