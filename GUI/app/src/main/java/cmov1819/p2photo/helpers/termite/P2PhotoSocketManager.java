package cmov1819.p2photo.helpers.termite;

import android.os.AsyncTask;

import cmov1819.p2photo.helpers.managers.LogManager;
import pt.inesc.termite.wifidirect.SimWifiP2pDevice;

class P2PhotoSocketManager {
    private static final String SOCKET_MGR_TAG = "SOCKET MANAGER";
    private final P2PhotoWiFiDirectManager mWiFiDirectManager;

    public P2PhotoSocketManager(P2PhotoWiFiDirectManager wiFiDirectManagerDirectManager) {
        this.mWiFiDirectManager = wiFiDirectManagerDirectManager;
    }

    public void doSend(final SimWifiP2pDevice targetDevice, final byte[] data) {
        LogManager.logInfo(
                SOCKET_MGR_TAG, String.format("Trying to send data to %s", targetDevice.deviceName)
        );
        new SendDataTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, mWiFiDirectManager, targetDevice, data);
    }
}
