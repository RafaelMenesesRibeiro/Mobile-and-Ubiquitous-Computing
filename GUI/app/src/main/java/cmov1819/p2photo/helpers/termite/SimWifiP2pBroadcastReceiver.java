package cmov1819.p2photo.helpers.termite;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import cmov1819.p2photo.MainMenuActivity;
import cmov1819.p2photo.helpers.managers.LogManager;
import pt.inesc.termite.wifidirect.SimWifiP2pBroadcast;
import pt.inesc.termite.wifidirect.SimWifiP2pDeviceList;
import pt.inesc.termite.wifidirect.SimWifiP2pInfo;

import static android.widget.Toast.LENGTH_SHORT;
import static android.widget.Toast.makeText;
import static pt.inesc.termite.wifidirect.SimWifiP2pBroadcast.EXTRA_DEVICE_LIST;
import static pt.inesc.termite.wifidirect.SimWifiP2pBroadcast.EXTRA_GROUP_INFO;
import static pt.inesc.termite.wifidirect.SimWifiP2pBroadcast.WIFI_P2P_GROUP_OWNERSHIP_CHANGED_ACTION;
import static pt.inesc.termite.wifidirect.SimWifiP2pBroadcast.WIFI_P2P_NETWORK_MEMBERSHIP_CHANGED_ACTION;
import static pt.inesc.termite.wifidirect.SimWifiP2pBroadcast.WIFI_P2P_PEERS_CHANGED_ACTION;
import static pt.inesc.termite.wifidirect.SimWifiP2pBroadcast.WIFI_P2P_STATE_CHANGED_ACTION;

public class SimWifiP2pBroadcastReceiver extends BroadcastReceiver {
    private static final String BROADCAST_RECEIVER_TAG = "BROADCAST RECEIVER";
    private MainMenuActivity activity;

    public SimWifiP2pBroadcastReceiver(MainMenuActivity activity) {
        super();
        this.activity = activity;
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        LogManager.logInfo(BROADCAST_RECEIVER_TAG, "Received termite notification...");

        String action = intent.getAction();

        LogManager.logInfo(BROADCAST_RECEIVER_TAG, "Action: " + action);

        if (WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
            int state = intent.getIntExtra(SimWifiP2pBroadcast.EXTRA_WIFI_STATE, -1);
            if (state == SimWifiP2pBroadcast.WIFI_P2P_STATE_ENABLED) {
                LogManager.logInfo(BROADCAST_RECEIVER_TAG, "state = p2p state enabled");
            } else {
                LogManager.logInfo(BROADCAST_RECEIVER_TAG, "state = p2p state disabled");
            }
        } else if (WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
            activity.requestPeers();
            LogManager.logInfo(BROADCAST_RECEIVER_TAG, "Peer list changed");
        } else if (WIFI_P2P_NETWORK_MEMBERSHIP_CHANGED_ACTION.equals(action)) {
            activity.requestGroupInfo();
            LogManager.logInfo(BROADCAST_RECEIVER_TAG, "Network membership changed");
        } else if (WIFI_P2P_GROUP_OWNERSHIP_CHANGED_ACTION.equals(action)) {
            activity.requestGroupInfo();
            LogManager.logInfo(BROADCAST_RECEIVER_TAG, "Group ownership changed");
        }
    }
}
