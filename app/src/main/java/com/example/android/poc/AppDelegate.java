package com.example.android.poc;

import android.app.Application;
import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;

/**
 * Created by User154 on 19-09-2017.
 */

public class AppDelegate extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
    }

    private AlertDialog dialog;


    public AlertDialog displayAlertDialog(final Context context, int msgId, DialogInterface.OnClickListener listener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.alert);
        builder.setMessage(msgId);
        builder.setCancelable(false);
        builder.setPositiveButton(R.string.ok, listener);
        if (dialog == null || !dialog.isShowing()) {
            dialog = builder.create();
            dialog.show();
        }
        return dialog;

    }
    public void dismissDialog() {
        if (dialog != null && dialog.isShowing())
            dialog.dismiss();
    }
}
