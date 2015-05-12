package com.rumly.pocketlibj;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

public final class Dialogs {
    public static void showErrorDialog(Context context, String title, String message) {
    	AlertDialog ad = new AlertDialog.Builder(context).create();
    	ad.setTitle(title);
    	ad.setIcon(android.R.drawable.ic_dialog_alert);
    	ad.setMessage(message);
    	ad.setButton(DialogInterface.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
			}
		});
    	ad.show();
    }
}
