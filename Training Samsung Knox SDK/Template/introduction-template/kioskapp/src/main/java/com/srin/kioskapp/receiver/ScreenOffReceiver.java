package com.srin.kioskapp.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.srin.kioskapp.view.ScreenSaverActivity;

public class ScreenOffReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if(Intent.ACTION_SCREEN_OFF.equals(action)){
//            KnoxActivation.getInstance(context).pushPowerButton();
        }
    }

    private void openNewActivity(Context context) {
        Intent intent = new Intent(context, ScreenSaverActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);
    }
}
