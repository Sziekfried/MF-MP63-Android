package com.sziegui.mpostag424.Utils;

import android.content.Context;
import android.view.View;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;

import cn.pedant.SweetAlert.SweetAlertDialog;

public class UtilsUi {
    private static UtilsUi instance;
    private Context context;

    private UtilsUi(Context ctx) {
        this.context = ctx;
    }
    public static synchronized UtilsUi getInstance(Context context) {
        if (instance == null) {
            instance = new UtilsUi(context.getApplicationContext());
        }
        return instance;
    }
    public void showSnackbarMsg(View view, String message) {
        Snackbar.make(view, message, Snackbar.LENGTH_SHORT).show();
    }

    public void showToastMsg(String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }
}
