package cmov1819.p2photo.helpers.callables;

import org.json.JSONObject;
import java.util.concurrent.Callable;
import pt.inesc.termite.wifidirect.SimWifiP2pDevice;

public class RequestPhotoToPeer implements Callable<JSONObject> {
    private SimWifiP2pDevice device;

    public RequestPhotoToPeer(SimWifiP2pDevice device) {
        this.device = device;
    }

    @Override
    public JSONObject call() throws Exception {
        return (JSONObject) getResponseMessage(connection, Expect.GOOD_STATE_RESPONSE);
    }
}
