package com.esdevices.backbeater.utils;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.StringRes;
import com.esdevices.backbeater.R;

/**
 * Created by Alina Kholcheva on 2017-02-16.
 */

public class DialogHelper {
    
    public static void showNoNetworkMessage(Context context){
        showMessage(R.string.dlg_no_connection_ttl, R.string.dlg_no_connection_msg, context);
    }
    
    public static void showMessage(@StringRes int titleId, @StringRes int msgId, Context context) {
        showMessage(context.getString(titleId), context.getString(msgId), context);
    }
    
    public static Dialog showMessage(String title, String msg, Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title);
        builder.setMessage(msg);
        builder.setPositiveButton(context.getString(R.string.OK), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        Dialog dialog = builder.show();
        return dialog;
    }



}
