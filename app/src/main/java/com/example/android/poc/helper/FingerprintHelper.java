package com.example.android.poc.helper;

import android.content.Context;
import android.hardware.fingerprint.FingerprintManager;
import android.os.CancellationSignal;

import com.example.android.poc.listener.AuthListener;

public class FingerprintHelper extends FingerprintManager.AuthenticationCallback {


    private final AuthListener listener;
    private Context context;


    // Constructor
    public FingerprintHelper(Context mContext, AuthListener listener) {
        context = mContext;
        this.listener = listener;
    }


    public void initAuth(FingerprintManager manager, FingerprintManager.CryptoObject cryptoObject) {
        CancellationSignal cancellationSignal = new CancellationSignal();
        // Now the protection level of USE_FINGERPRINT permission is normal instead of dangerous.
        // See http://developer.android.com/reference/android/Manifest.permission.html#USE_FINGERPRINT
        // The line below prevents the false positive inspection from Android Studio.
        manager.authenticate(cryptoObject, cancellationSignal, 0, this, null);
    }


    @Override
    public void onAuthenticationError(int errMsgId, CharSequence errString) {
        listener.fingerScanned("Fingerprint Authentication error\n" + errString, false);
    }


    @Override
    public void onAuthenticationHelp(int helpMsgId, CharSequence helpString) {
        listener.fingerScanned("Fingerprint Authentication help\n" + helpString, false);
    }


    @Override
    public void onAuthenticationFailed() {
        listener.fingerScanned("Fingerprint Authentication failed.", false);
    }


    @Override
    public void onAuthenticationSucceeded(FingerprintManager.AuthenticationResult result) {
        listener.fingerScanned("Fingerprint Authentication succeeded.", true);
    }


}