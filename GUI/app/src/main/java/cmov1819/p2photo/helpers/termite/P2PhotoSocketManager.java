package cmov1819.p2photo.helpers.termite;

import android.os.AsyncTask;

import pt.inesc.termite.wifidirect.SimWifiP2pDevice;

class P2PhotoSocketManager {

    private final P2PhotoWiFiDirectManager mWiFiDirectManager;

    public P2PhotoSocketManager(P2PhotoWiFiDirectManager wiFiDirectManagerDirectManager) {
        this.mWiFiDirectManager = wiFiDirectManagerDirectManager;
    }

    public void doSend(final SimWifiP2pDevice targetDevice, final byte[] data) {
        new SendDataTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, mWiFiDirectManager, targetDevice, data);
    }
}
