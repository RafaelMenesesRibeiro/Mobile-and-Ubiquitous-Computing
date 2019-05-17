package cmov1819.p2photo.helpers.callables;

import org.json.JSONObject;
import java.util.concurrent.Callable;
import pt.inesc.termite.wifidirect.SimWifiP2pDevice;

public class GetPhotoFromPeerCallable implements Callable<JSONObject> {
    private SimWifiP2pDevice device;

    public GetPhotoFromPeerCallable(SimWifiP2pDevice device) {
        this.device = device;
    }

    @Override
    public JSONObject call() throws Exception {
        return null;
    }
}
