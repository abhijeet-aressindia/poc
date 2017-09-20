package com.example.android.poc;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import com.example.android.poc.listener.WiFistateListener;

public class WifiReceiver extends BroadcastReceiver {

    private final WiFistateListener listener;

    public WifiReceiver(WiFistateListener listener) {
        this.listener = listener;
    }


    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        if (action.equals(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION)) {
            checkConnectedWifi(context);
        }
    }

    /**
     * Detect you are connected to a specific network.
     */
    private void checkConnectedWifi(Context context) {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifi = wifiManager.getConnectionInfo();
        if (wifi != null && wifi.getSupplicantState() == SupplicantState.COMPLETED) {
            if (listener != null)
                listener.wifiStateConnected(wifi.getBSSID(), wifi.getSSID());
            Log.e("bssid", "bssid " + wifi.getBSSID());
        }
    }
}