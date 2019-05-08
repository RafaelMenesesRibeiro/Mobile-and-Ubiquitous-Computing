package cmov1819.p2photo.helpers.termite;

import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import cmov1819.p2photo.MainMenuActivity;
import cmov1819.p2photo.R;
import pt.inesc.termite.wifidirect.sockets.SimWifiP2pSocket;
import pt.inesc.termite.wifidirect.sockets.SimWifiP2pSocketServer;

public class IncommingSocketTask extends AsyncTask<Object, String, Void> {

    private static final String INCOMMING_TASK_TAG = "INCOMMING SOCKET";

    @Override
    protected void onPreExecute() {
        // TODO if pre processing is required
    }

    @Override
    protected Void doInBackground(final Object... params) {

        Log.d(INCOMMING_TASK_TAG, "Started incomming communication task (" + this.hashCode() + ").");

        try {
            MainMenuActivity requestingActivity = (MainMenuActivity) params[0];
            // setup a server socket on MainMenuActivity
            requestingActivity.setServerSocket(new SimWifiP2pSocketServer(R.string.termite_port));
            // set server socket to listen to incomming requests indefinetly
            while (!Thread.currentThread().isInterrupted()) {
                SimWifiP2pSocket socket = requestingActivity.getServerSocket().accept();
                try {
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    publishProgress(bufferedReader.readLine());
                    socket.getOutputStream().write(("\n").getBytes());
                } catch (IOException ioe) {
                    Log.e(INCOMMING_TASK_TAG, "Error reading socket: " + ioe.getMessage());
                } finally {
                    socket.close();
                }
            }
        } catch (IOException ioe) {
            Log.e(INCOMMING_TASK_TAG, "Socket error: " + ioe.getMessage());
        }

        return null;
    }

    @Override
    protected void onPostExecute(Void result) {
        // TODO investigate if we want to do something after doInBackground ends
    }

}