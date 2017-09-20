package com.example.android.poc;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.databinding.DataBindingUtil;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.view.View;
import android.widget.Toast;

import com.example.android.poc.database.DatabaseHelper;
import com.example.android.poc.databinding.ActivityMainBinding;
import com.example.android.poc.listener.WiFistateListener;

public class WifiConfigurationActivity extends AppCompatActivity implements View.OnClickListener, WiFistateListener {

    private static final String TAG = "WifiConfigActivity";
    ActivityMainBinding binding;
    private String mSSID;
    private String mBSSID;
    private WifiReceiver mWifiReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getSupportActionBar() != null)
            getSupportActionBar().setTitle(R.string.configure_wifi);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        mWifiReceiver = new WifiReceiver(this);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
        registerReceiver(new WifiReceiver(this), intentFilter);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            binding.txtNote.setText(Html.fromHtml(getString(R.string.note_msg), Html.FROM_HTML_MODE_LEGACY));
        } else binding.txtNote.setText(Html.fromHtml(getString(R.string.note_msg)));
        binding.btnChangeWifi.setOnClickListener(this);
        binding.btnContinue.setOnClickListener(this);


    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mWifiReceiver, new IntentFilter(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION));
        ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connManager.getActiveNetworkInfo();
        if (networkInfo == null || (networkInfo.isConnected() && networkInfo.getType() != ConnectivityManager.TYPE_WIFI)) {
            ((AppDelegate) getApplicationContext()).displayAlertDialog(this, R.string.error_msg_connect_to_wifi, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    startActivity(new Intent(WifiManager.ACTION_PICK_WIFI_NETWORK));
                }
            });
        } else {
            final WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            final WifiInfo connectionInfo = wifiManager.getConnectionInfo();
            if (connectionInfo != null && !(connectionInfo.getSSID().equals(""))) {
                mSSID = connectionInfo.getSSID();
                mBSSID = connectionInfo.getBSSID();
                binding.txtSSDName.setText(mSSID);

            }
        }
    }


    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_change_wifi:
                startActivity(new Intent(WifiManager.ACTION_PICK_WIFI_NETWORK));
                break;
            case R.id.btn_continue:
                if (mBSSID != null && mSSID != null) {
                    new DatabaseHelper(this).saveWIFI(mSSID, mBSSID);
                    finish();
                } else {
                    Toast.makeText(this, R.string.error_msg_went_wrong, Toast.LENGTH_SHORT).show();
                }
                break;
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mWifiReceiver);
    }


    @Override
    public void wifiStateConnected(String bssid, String ssid) {
        mSSID = ssid;
        mBSSID = bssid;
        if (mBSSID != null)
            binding.txtSSDName.setText(mSSID);
        ((AppDelegate) getApplicationContext()).dismissDialog();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finishAffinity();
    }
}
