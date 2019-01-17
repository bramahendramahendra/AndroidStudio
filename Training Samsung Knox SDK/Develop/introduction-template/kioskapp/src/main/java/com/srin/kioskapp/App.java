package com.srin.kioskapp;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;

import com.srin.kioskapp.receiver.ScreenOffReceiver;

import java.lang.ref.WeakReference;

public class App extends Application {

    private static App app;
    private WeakReference<Boolean> isVideoShowing;
    private BroadcastReceiver mScreenStateReceiver;

    @Override
    public void onCreate() {
        super.onCreate();
        app = this;
        mScreenStateReceiver = new ScreenOffReceiver();
        IntentFilter screenStateFilter = new IntentFilter();
        screenStateFilter.addAction(Intent.ACTION_SCREEN_ON);
        screenStateFilter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(mScreenStateReceiver, screenStateFilter);
        isVideoShowing = new WeakReference<>(false);
    }

    @Override
    public void onTerminate() {
        super.onTerminate();

    }

    public static boolean isVideoShowing() {
        return app.isVideoShowing.get();
    }

    public static void setVideoShowing(boolean showing) {
        app.isVideoShowing = new WeakReference<>(showing);
    }


}
