package com.srin.kioskapp.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.util.Log;

import com.srin.kioskapp.App;
import com.srin.kioskapp.controller.KnoxActivation;
import com.srin.kioskapp.view.ScreenSaverActivity;

import java.io.File;

import static com.srin.kioskapp.view.ScreenSaverActivity.VIDEO_DIR_PATH;

public class UserActivityReceiver extends BroadcastReceiver {

    private static final String ACTION_USER_ACTIVITY = "com.samsung.android.knox.intent.action.USER_ACTIVITY";
    private static final String ACTION_USER_ACTIVITY_OLD = "com.sec.action.USER_ACTIVITY";
    private static final String ACTION_USER_NO_ACTIVITY = "com.samsung.android.knox.intent.action.NO_USER_ACTIVITY";
    private static final String ACTION_USER_NO_ACTIVITY_OLD = "com.sec.action.NO_USER_ACTIVITY";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if(ACTION_USER_NO_ACTIVITY.equals(action) || ACTION_USER_NO_ACTIVITY_OLD.equals(action)){
            Log.d(UserActivityReceiver.class.getSimpleName(), "ACTION_USER_NO_ACTIVITY");
            File file = new File(Environment.getExternalStorageDirectory(), VIDEO_DIR_PATH);
            if(file.listFiles() == null) return;
            if(file.listFiles().length<1) return;
            if(!App.isVideoShowing() && KnoxActivation.isELMLicenseActive(context)){
                openNewActivity(context);
            }

        }

    }

    private void openNewActivity(Context context) {
        Intent intent = new Intent(context, ScreenSaverActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);
    }
}

