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

        /*TODO initiate Kiosk Mode instance*/

	}

	public static Controller getInstance(Context context) {
		if (null == mInstance) {
			mInstance = new Controller(context);
		}

		return mInstance;
	}


	public void enableKioskMode(String pkgName){
		/*TODO Implement enable kiosk mode*/

	}

	public boolean isKioskModeEnabled(){
		return mKioskMode.isKioskModeEnabled();
	}

	public void disableKioskMode(){
		/*TODO Implement disable kiosk mode*/
	}

	public static class KioskModeReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			/*TODO handle kiosk mode receiver*/
		}
	}

	public void disableHardwareKey(boolean disable, int hwKey) {
		/*TODO Implement toggle hardware keys*/
	}

	public void hideStatusBar(boolean state){
		/*TODO Implement toggle status bar*/
	}

	public void setUserTimeOut(int userTimeOut) {
		/*TODO Implement set user inactivity timeout*/
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
	}

	public void changeShutdownAnimation() {
		/*TODO : Implement change shutdown animation*/
	}

	public void powerOff() {
		/*TODO Implement power off device */
	}

	public void setForceAutoStartUpState(int state) {
		/*TODO Implement auto on device*/
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
