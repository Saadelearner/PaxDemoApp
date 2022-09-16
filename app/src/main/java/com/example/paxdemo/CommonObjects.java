package com.example.paxdemo;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Handler;
import android.os.Message;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CommonObjects {

    public static Handler handler;

    public static void showProgress(Context context, String message) {
        progress = ProgressDialog.show(context, null, message, true);
        handler = new Handler();
        handler.postDelayed(hideProgressTask, 20000);
//        hand
    }

    public static void hideProgress() {
        try {
            if (progress != null)
                progress.dismiss();
        } catch (Exception e) {

        }
    }

    public static ProgressDialog progress;
    static Runnable hideProgressTask = new Runnable() {
        @Override
        public void run() {
            Message alertMessage = new Message();
            alertMessage.what = 1;

            handlerMesg.sendMessage(alertMessage);
        }
    };
    static Handler handlerMesg = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            try {
                if (progress != null && progress.isShowing()) {
                    progress.dismiss();
                    handler.removeCallbacks(hideProgressTask);
                    handler = null;
                }
            } catch (Exception e) {

            }
        }
    };
}

