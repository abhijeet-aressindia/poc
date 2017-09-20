package com.example.android.poc;

import android.app.KeyguardManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.databinding.DataBindingUtil;
import android.hardware.fingerprint.FingerprintManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyPermanentlyInvalidatedException;
import android.security.keystore.KeyProperties;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import com.example.android.poc.database.DatabaseHelper;
import com.example.android.poc.databinding.ActivityHomeBinding;
import com.example.android.poc.helper.FingerprintHelper;
import com.example.android.poc.listener.AuthListener;
import com.example.android.poc.listener.WiFistateListener;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Calendar;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

public class HomeActivity extends AppCompatActivity implements WiFistateListener, AuthListener {

    private static final String CONST_KEY_NAME = "poc";
    private DatabaseHelper mDatabaseHelper;
    private WifiReceiver mWifiReceiver;
    private KeyStore keyStore;
    private Cipher cipher;
    private ActivityHomeBinding binding;
    private FingerprintHelper fingerprintHelper;
    private AlertDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_home);
        mDatabaseHelper = new DatabaseHelper(this);
        mWifiReceiver = new WifiReceiver(this);

    }

    private boolean checkIfSameDay(long timestamp) {
        int dbDay = 0, currentDay = 0;
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(timestamp);
        dbDay += c.get(Calendar.DAY_OF_MONTH);
        dbDay += c.get(Calendar.MONTH);
        dbDay += c.get(Calendar.YEAR);

        c.setTimeInMillis(System.currentTimeMillis());
        currentDay += c.get(Calendar.DAY_OF_MONTH);
        currentDay += c.get(Calendar.MONTH);
        currentDay += c.get(Calendar.YEAR);

        return dbDay == currentDay;
    }


    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mWifiReceiver, new IntentFilter(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION));
        if (!mDatabaseHelper.isWifiSaved()) {
            startActivity(new Intent(this, WifiConfigurationActivity.class));
        } else {
            setUpAuthentication();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mWifiReceiver);
    }

    @Override
    public void wifiStateConnected(String bssid, String ssid) {
        if (!mDatabaseHelper.getWifiBSSID().equals(bssid)) {
            dialog = ((AppDelegate) getApplicationContext()).displayAlertDialog(this, R.string.error_msg_connect_to_wifi, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    startActivity(new Intent(WifiManager.ACTION_PICK_WIFI_NETWORK));
                }
            });
        } else if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }

    }

    @Override
    public void fingerScanned(String msg, boolean result) {
        binding.txtAuthMsg.setVisibility(View.VISIBLE);
        if (result) {
            binding.txtAuthMsg.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_fingerprint_success, 0, 0, 0);
            mDatabaseHelper.saveAttendance();
            msg = msg + getString(R.string.att_marked);
            fingerprintHelper = null;
        } else
            binding.txtAuthMsg.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_fingerprint_error, 0, 0, 0);
        binding.txtAuthMsg.setText(msg);

    }

    private void setUpAuthentication() {
        if (fingerprintHelper != null) return;
        KeyguardManager keyguardManager = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
        FingerprintManager fingerprintManager = (FingerprintManager) getSystemService(FINGERPRINT_SERVICE);

        long timestamp = 0L;
        if ((timestamp = mDatabaseHelper.getAttendance()) > 0 && checkIfSameDay(timestamp)) {
            binding.txtAuthMsg.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_fingerprint_success, 0, 0, 0);
            binding.txtAuthMsg.setText(R.string.attence_is_marked);
            binding.txtAuthMsg.setVisibility(View.VISIBLE);
        }
        // Now the protection level of USE_FINGERPRINT permission is normal instead of dangerous.
        // See http://developer.android.com/reference/android/Manifest.permission.html#USE_FINGERPRINT
        // The line below prevents the false positive inspection from Android Studio.
        else if (!fingerprintManager.isHardwareDetected()) {
            Toast.makeText(this, R.string.error_msg_device_not_support_hardware, Toast.LENGTH_LONG).show();
        } else {
            if (!fingerprintManager.hasEnrolledFingerprints()) {
                ((AppDelegate) getApplicationContext()).displayAlertDialog(this, R.string.error_msg_register_fingerprint, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        startActivity(new Intent(android.provider.Settings.ACTION_SECURITY_SETTINGS));
                    }
                });

            } else {
                if (!keyguardManager.isKeyguardSecure())
                    Toast.makeText(this, getString(R.string.error_msg_keygaurd_disabled)
                                    ,
                            Toast.LENGTH_LONG).show();
                else {
                    generateKey();
                    if (getCipher()) {
                        FingerprintManager.CryptoObject cryptoObject = new FingerprintManager.CryptoObject(cipher);
                        fingerprintHelper = new FingerprintHelper(this, this);
                        fingerprintHelper.initAuth(fingerprintManager, cryptoObject);
                    }
                }
            }
        }
    }

    /**
     * This method generate key from Android Keystore.
     */
    private void generateKey() {
        try {
            keyStore = KeyStore.getInstance("AndroidKeyStore");
        } catch (Exception e) {
            e.printStackTrace();
        }
        KeyGenerator keyGenerator;
        try {
            keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            throw new RuntimeException("Failed to get KeyGenerator instance", e);
        }

        try {
            keyStore.load(null);
            keyGenerator.init(new
                    KeyGenParameterSpec.Builder(CONST_KEY_NAME,
                    KeyProperties.PURPOSE_ENCRYPT |
                            KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                    .setUserAuthenticationRequired(true)
                    .setEncryptionPaddings(
                            KeyProperties.ENCRYPTION_PADDING_PKCS7)
                    .build());
            keyGenerator.generateKey();
        } catch (NoSuchAlgorithmException |
                InvalidAlgorithmParameterException
                | CertificateException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean getCipher() {
        try {
            cipher = Cipher.getInstance(KeyProperties.KEY_ALGORITHM_AES + "/" + KeyProperties.BLOCK_MODE_CBC + "/" + KeyProperties.ENCRYPTION_PADDING_PKCS7);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new RuntimeException("Failed to get Cipher", e);
        }
        try {
            keyStore.load(null);
            SecretKey key = (SecretKey) keyStore.getKey(CONST_KEY_NAME,
                    null);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            return true;
        } catch (KeyPermanentlyInvalidatedException e) {
            return false;
        } catch (KeyStoreException | CertificateException | UnrecoverableKeyException | IOException | NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Failed to init Cipher", e);
        }
    }


}
