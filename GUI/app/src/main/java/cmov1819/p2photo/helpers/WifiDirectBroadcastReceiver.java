package cmov1819.p2photo.helpers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import cmov1819.p2photo.MainMenuActivity;
import pt.inesc.termite.wifidirect.SimWifiP2pBroadcast;
import pt.inesc.termite.wifidirect.SimWifiP2pInfo;

import static android.widget.Toast.LENGTH_SHORT;
import static android.widget.Toast.makeText;
import static pt.inesc.termite.wifidirect.SimWifiP2pBroadcast.EXTRA_GROUP_INFO;
import static pt.inesc.termite.wifidirect.SimWifiP2pBroadcast.WIFI_P2P_GROUP_OWNERSHIP_CHANGED_ACTION;
import static pt.inesc.termite.wifidirect.SimWifiP2pBroadcast.WIFI_P2P_NETWORK_MEMBERSHIP_CHANGED_ACTION;
import static pt.inesc.termite.wifidirect.SimWifiP2pBroadcast.WIFI_P2P_PEERS_CHANGED_ACTION;
import static pt.inesc.termite.wifidirect.SimWifiP2pBroadcast.WIFI_P2P_STATE_CHANGED_ACTION;

public class WifiDirectBroadcastReceiver extends BroadcastReceiver {
    private MainMenuActivity activity;

    public WifiDirectBroadcastReceiver(MainMenuActivity activity) {
        super();
        this.activity = activity;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
            int state = intent.getIntExtra(SimWifiP2pBroadcast.EXTRA_WIFI_STATE, -1);
            if (state == SimWifiP2pBroadcast.WIFI_P2P_STATE_ENABLED) {
                makeText(activity,"WiFi Direct enabled", LENGTH_SHORT).show();
            } else {
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
