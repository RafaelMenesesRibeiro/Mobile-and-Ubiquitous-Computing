package cmov1819.p2photo.helpers.termite;

import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;

import cmov1819.p2photo.MainMenuActivity;
import cmov1819.p2photo.R;
import pt.inesc.termite.wifidirect.sockets.SimWifiP2pSocket;

public class OutgoingSocketTask extends AsyncTask<Object, Void, String> {

    private static final String OUTGOING_TASK_TAG = "OUTGOING COMMUNICATION";

    @Override
    protected void onPreExecute() {
        // TODO if pre processing is required
    }

    @Override
    protected String doInBackground(final Object... params) {
        try {
            MainMenuActivity requestingActivity = (MainMenuActivity) params[0];
            String destinationAddress = (String) params[1];
            requestingActivity.setClientSocket(new SimWifiP2pSocket(destinationAddress, R.string.termite_port)) ;
        } catch (IOException exc) {
            Log.d(OUTGOING_TASK_TAG, "Error: " + exc.getMessage());
        }
        return null;
    }
}