package com.srin.kioskapp.controller;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Toast;

import com.samsung.android.knox.EnterpriseDeviceManager;
import com.samsung.android.knox.custom.CustomDeviceManager;
import com.samsung.android.knox.custom.SystemManager;
import com.samsung.android.knox.kiosk.KioskMode;
import com.samsung.android.knox.restriction.RestrictionPolicy;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;

import static com.srin.kioskapp.view.ScreenSaverActivity.VIDEO_DIR_PATH;

public class Controller {

	private static final String TAG = Controller.class.getSimpleName();
	private static Controller mInstance;
	private Context mContext;
	private Toast mToast;

	private EnterpriseDeviceManager mEDM;
	private KioskMode mKioskMode;

	@SuppressLint("ShowToast")
	public Controller(Context context) {
		mContext = context;
		mToast = Toast.makeText(mContext, "", Toast.LENGTH_SHORT);

		/*TODO initiate Enterprise Device Manager instance*/
		mEDM = EnterpriseDeviceManager.getInstance(mContext);
        /*TODO initiate Kiosk Mode instance*/
		mKioskMode = mEDM.getKioskMode();

	}

	public static Controller getInstance(Context context) {
		if (null == mInstance) {
			mInstance = new Controller(context);
		}

		return mInstance;
	}


	public void enableKioskMode(String pkgName){
		/*TODO Implement enable kiosk mode*/
		try {
			if(!isKioskModeEnabled()) {
				Log.w("TRACE", "enableKioskMode: ");
				mKioskMode.enableKioskMode(pkgName);
			}
		} catch (SecurityException e) {
			Log.w(TAG, "SecurityException: " + e);
		}
	}

	public boolean isKioskModeEnabled(){
		return mKioskMode.isKioskModeEnabled();
	}

	public void disableKioskMode(){
		/*TODO Implement disable kiosk mode*/
		try {
			Log.w("TRACE", "disableKioskMode: ");
			mKioskMode.disableKioskMode();
		} catch (SecurityException e) {
			Log.w(TAG, "SecurityException: " + e);
		}
	}

	public static class KioskModeReceiver extends BroadcastReceiver {
		/*TODO handle kiosk mode receiver*/
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(KioskMode.ACTION_ENABLE_KIOSK_MODE_RESULT))
			{
				final int kioskResult =
						intent.getExtras().getInt(KioskMode.EXTRA_KIOSK_RESULT);
				if (kioskResult == KioskMode.ERROR_NONE) {
					Log.w("TRACE", "enableKioskMode: success");
					//Do something
					Controller.getInstance(context).disableHardwareKey(true,
							KeyEvent.KEYCODE_HOME);
					Controller.getInstance(context).disableHardwareKey(true,
							KeyEvent.KEYCODE_BACK);
					Controller.getInstance(context).disableHardwareKey(true,
							KeyEvent.KEYCODE_VOLUME_UP);
					Controller.getInstance(context).disableHardwareKey(true,
							KeyEvent.KEYCODE_VOLUME_DOWN);
					Controller.getInstance(context).disableHardwareKey(true,
							KeyEvent.KEYCODE_MENU);
					Controller.getInstance(context).disableHardwareKey(true,
							KeyEvent.KEYCODE_APP_SWITCH);
					Controller.getInstance(context).hideStatusBar(true);
					Controller.getInstance(context).setUserTimeOut(10);
				} else {
					Toast.makeText(context, "Kiosk result error " + kioskResult,
							Toast.LENGTH_SHORT).show();
				}
			} else if
					(intent.getAction().equals(KioskMode.ACTION_DISABLE_KIOSK_MODE_RESULT)) {
				final int kioskResult =
						intent.getExtras().getInt(KioskMode.EXTRA_KIOSK_RESULT);
				if (kioskResult == KioskMode.ERROR_NONE) {
					Log.w("TRACE", "disableKioskMode: success");
					//Do something
					Controller.getInstance(context).disableHardwareKey(false,
							KeyEvent.KEYCODE_HOME);
					Controller.getInstance(context).disableHardwareKey(false,
							KeyEvent.KEYCODE_BACK);
					Controller.getInstance(context).disableHardwareKey(false,
							KeyEvent.KEYCODE_VOLUME_UP);
					Controller.getInstance(context).disableHardwareKey(false,
							KeyEvent.KEYCODE_VOLUME_DOWN);
					Controller.getInstance(context).disableHardwareKey(false,
							KeyEvent.KEYCODE_MENU);
					Controller.getInstance(context).disableHardwareKey(false,
							KeyEvent.KEYCODE_APP_SWITCH);
					Controller.getInstance(context).hideStatusBar(false);
				}
			}
		}
	}


	public void disableHardwareKey(boolean disable, int hwKey) {
		/*TODO Implement toggle hardware keys*/
		try {
			if (mKioskMode.allowHardwareKeys(new
							ArrayList<Integer>(Arrays.asList(new Integer[]{hwKey})),
					!disable).contains(hwKey)) {
				mToast.setText(String.format("%s Hardware Keys %s success", disable
						? "Disable" : "Enable", KeyEvent.keyCodeToString(hwKey).replace("KEYCODE_",
						"")));
			} else {
				mToast.setText(String.format("%s Hardware Keys %s failed", disable ?
						"Disable" : "Enable", KeyEvent.keyCodeToString(hwKey).replace("KEYCODE_", "")));
			}
		} catch (SecurityException e) {
			Log.w(TAG, "SecurityException: " + e);
			mToast.setText("Error: SecurityException occurred - " + e);
			mToast.show();
		}
	}

	public void hideStatusBar(boolean state){
		/*TODO Implement toggle status bar*/
		try {
			mKioskMode.hideStatusBar(state);
		} catch (SecurityException e) {
			Log.w(TAG, "SecurityException: " + e);
		}
	}


	public void setUserTimeOut(int userTimeOut) {
		/*TODO Implement set user inactivity timeout*/
		try {
			CustomDeviceManager cdm = CustomDeviceManager.getInstance();
			SystemManager systemManager = cdm.getSystemManager();
			int result = systemManager.setUserInactivityTimeout(userTimeOut);
			Log.d(TAG, result== 1 ?
					"set timeout success " : "set timeout failed");
			int screenResult = systemManager.setScreenTimeout(10);
			Log.d(TAG, "set screen timeout result : "+screenResult);
		} catch(SecurityException e) {
			Log.w(TAG, "SecurityException:" + e);
		}catch (Exception ee){
			ee.printStackTrace();
		}
	}


	public static class PowerConnectionReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			if(intent.getAction().equals("android.intent.action.ACTION_POWER_CONNECTED")) {
				Log.d(TAG,"ACTION_POWER_CONNECTED");
			}else if(intent.getAction().equals("android.intent.action.ACTION_POWER_DISCONNECTED")) {
				Log.d(TAG,"ACTION_POWER_DISCONNECTED");
				Controller.getInstance(context).powerOff();
			}
		}
	}

	public void changeBootAnimation() {
		/*TODO : Implement change boot animation*/
		CustomDeviceManager cdm = CustomDeviceManager.getInstance();
		SystemManager systemManager = cdm.getSystemManager();
		try {
			String animationFile = mContext.getFilesDir() + "/bootsamsung.qmg";
			String loopFile = mContext.getFilesDir() + "/bootsamsungloop.qmg";
			String soundFile = mContext.getFilesDir() + "/sound.ogg";
			File fileAnimation = new File(animationFile);
			fileAnimation.setReadable(true, false);
			ParcelFileDescriptor animationFD = ParcelFileDescriptor.open(fileAnimation,
					ParcelFileDescriptor.MODE_READ_ONLY);
			File fileLoop = new File(loopFile);
			fileLoop.setReadable(true, false);
			ParcelFileDescriptor loopFD = ParcelFileDescriptor.open(fileLoop,
					ParcelFileDescriptor.MODE_READ_ONLY);
			File fileSound = new File(soundFile);
			fileSound.setReadable(true, false);
			ParcelFileDescriptor soundFD = ParcelFileDescriptor.open(fileSound,
					ParcelFileDescriptor.MODE_READ_ONLY);
			int error = systemManager.setBootingAnimation(animationFD, loopFD, soundFD,
					2000);
			mToast.setText("errorcode "+ error);
			mToast.show();
		} catch(FileNotFoundException e) {
			Log.w(TAG, "FileNotFoundException:" + e);
		} catch(NoSuchMethodError e) {
			Log.w(TAG, "NoSuchMethodError:" + e);
		} catch (SecurityException e) {
			Log.w(TAG, "SecurityException:" + e);
		}
	}

	public void changeShutdownAnimation() {
		/*TODO : Implement change shutdown animation*/
		CustomDeviceManager cdm = CustomDeviceManager.getInstance();
		SystemManager systemManager = cdm.getSystemManager();
		try {
			String animationFile = mContext.getFilesDir() + "/shutdown.qmg";
			String soundFile = mContext.getFilesDir() + "/sound.ogg";
			File fileAnimation = new File(animationFile);
			fileAnimation.setReadable(true, false);
			ParcelFileDescriptor animationFD = ParcelFileDescriptor.open(fileAnimation,
					ParcelFileDescriptor.MODE_READ_ONLY);
			File fileSound = new File(soundFile);
			fileSound.setReadable(true, false);
			ParcelFileDescriptor soundFD = ParcelFileDescriptor.open(fileSound,
					ParcelFileDescriptor.MODE_READ_ONLY);
			int error = systemManager.setShuttingDownAnimation(animationFD, soundFD);
			mToast.setText("errorcode " + error);
			mToast.show();
		} catch(FileNotFoundException e) {
			Log.w(TAG, "FileNotFoundException:" + e);
		} catch(NoSuchMethodError e) {
			Log.w(TAG, "NoSuchMethodError:" + e);
		} catch(SecurityException e) {
			Log.w(TAG, "SecurityException:" + e);
		}
	}

	public void powerOff() {
		/*TODO Implement power off device */
		CustomDeviceManager cdm = CustomDeviceManager.getInstance();
		SystemManager systemManager = cdm.getSystemManager();
		try {
			systemManager.powerOff();
		}catch (NoSuchMethodError e) {
			Log.e(TAG,"NoSuchMethodError:" + e);
		}catch (SecurityException e) {
			Log.e(TAG,"SecurityException:" + e);
		}

	}

	public void setForceAutoStartUpState(int state) {
		/*TODO Implement auto on device*/
		CustomDeviceManager cdm = CustomDeviceManager.getInstance();
		SystemManager systemManager = cdm.getSystemManager();
		try {
			int code = systemManager.setForceAutoStartUpState(state);
			if(code == 0) {
				Toast.makeText(mContext,(systemManager.getForceAutoStartUpState() == 1 ?
						"Enable":"Disable") + " Set Auto Power On",Toast.LENGTH_LONG).show();
			}
		}catch (NoSuchMethodError e) {
			Log.e(TAG,"NoSuchMethodError:" + e);
		}catch (SecurityException e) {
			Log.e(TAG,"SecurityException:" + e);
		}
	}

	public void copyAssets(String path) {
		AssetManager assetManager = mContext.getAssets();
		String[] files = null;
			try {
			files = assetManager.list(path);
		} catch (IOException e) {
			Log.e("tag", "Failed to get asset file list.", e);
		}
		if (files != null) for (String filename : files) {
			InputStream in = null;
			OutputStream out = null;
			try {
				File outFile;
				if (path.length() > 1) {
					in = assetManager.open(path + "/" + filename);
					File contentDir = new File(Environment.getExternalStorageDirectory(), VIDEO_DIR_PATH);
					if(!contentDir.exists()) contentDir.mkdir();
					outFile = new File(Environment.getExternalStorageDirectory(), VIDEO_DIR_PATH + filename);
				} else {
					in = assetManager.open(filename);
					outFile = new File(mContext.getFilesDir(), filename);
				}
				if (outFile.exists()) continue;
				out = new FileOutputStream(outFile);
				copyFile(in, out);
			}catch(FileNotFoundException e) {
				Log.e("tag", "FileNotFoundException, Failed to copy asset file: " + filename, e);
			} catch(IOException e) {
				Log.e("tag", "IOException, Failed to copy asset file: " + filename, e);
			}
			finally {
				if (in != null) {
					try {
						in.close();
					} catch (IOException e) {
						// NOOP
					}
				}
				if (out != null) {
					try {
						out.close();
					} catch (IOException e) {
						// NOOP
					}
				}
			}
		}
	}
	private void copyFile(InputStream in, OutputStream out) throws IOException {
		byte[] buffer = new byte[1024];
		int read;
		while((read = in.read(buffer)) != -1){
			out.write(buffer, 0, read);
		}
	}


}
