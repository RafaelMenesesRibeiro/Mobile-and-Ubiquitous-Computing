package cmov1819.p2photo.helpers.termite;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import cmov1819.p2photo.MainMenuActivity;
import cmov1819.p2photo.helpers.managers.LogManager;
import pt.inesc.termite.wifidirect.SimWifiP2pBroadcast;
import pt.inesc.termite.wifidirect.SimWifiP2pInfo;

import static android.widget.Toast.LENGTH_SHORT;
import static android.widget.Toast.makeText;
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

        LogManager.logError(BROADCAST_RECEIVER_TAG, "Received termite notification...");

        String action = intent.getAction();

        LogManager.logError(BROADCAST_RECEIVER_TAG, "Action: " + action);

        if (WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
            int state = intent.getIntExtra(SimWifiP2pBroadcast.EXTRA_WIFI_STATE, -1);
            if (state == SimWifiP2pBroadcast.WIFI_P2P_STATE_ENABLED) {
                LogManager.logError("MAIN", "state = p2p state enabled");
                makeText(activity,"WiFi Direct enabled", LENGTH_SHORT).show();
            } else {
                LogManager.logError("MAIN", "state = p2p state disabled");
                makeText(activity,"WiFi Direct disabled", LENGTH_SHORT).show();
            }
        } else if (WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
            makeText(activity,"Peer list changed", LENGTH_SHORT).show();
        } else if (WIFI_P2P_NETWORK_MEMBERSHIP_CHANGED_ACTION.equals(action)) {
            SimWifiP2pInfo groupInfo = (SimWifiP2pInfo)intent.getSerializableExtra(EXTRA_GROUP_INFO);
            groupInfo.print();
            makeText(activity,"Network membership changed", LENGTH_SHORT).show();
        } else if (WIFI_P2P_GROUP_OWNERSHIP_CHANGED_ACTION.equals(action)) {
            SimWifiP2pInfo groupInfo = (SimWifiP2pInfo)intent.getSerializableExtra(EXTRA_GROUP_INFO);
            groupInfo.print();
            makeText(activity,"Group ownership changed", LENGTH_SHORT).show();
        }
    }
}
