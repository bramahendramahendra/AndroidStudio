package com.srin.kioskapp.controller;

import android.app.Activity;
import android.app.admin.DeviceAdminReceiver;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import com.samsung.android.knox.license.EnterpriseLicenseManager;
import com.samsung.android.knox.license.KnoxEnterpriseLicenseManager;

public class KnoxActivation {

    private static KnoxActivation mInstance;
    private static Context mContext;
    private Toast mToast;
    private ComponentName mDeviceAdmin;
    private DevicePolicyManager mDPM;
    private EnterpriseLicenseManager mELM;
    private KnoxEnterpriseLicenseManager mSKL;

    public static String LICENSEKNOX = "LICENSEKNOX";
    public static String ELMKEY = "ELMKEY";
    public static String SKLKEY = "SKLKEY";


    private static String TAG = KnoxActivation.class.getSimpleName();
    private String elmLicense = "94E51A2A93A6C626D5CE856857FE18DDAD6547A11B44054DAF9075AC55287A8818699A4538100DBA203C10F862E223E010DB500721A441F148C9B8DE72DCE557";// TODO Please use Backwards-compatible key
    private String sklLicense = "KLM06-6C206-SOEWP-ZSI0A-YPNZO-AU462";// TODO Please use SKL key

    private String pkgName;

    public KnoxActivation(Context context) {
        mContext = context;
        mToast = Toast.makeText(mContext, "", Toast.LENGTH_SHORT);
        mDeviceAdmin = new ComponentName(mContext, KnoxAdmin.class);
        pkgName = context.getPackageName();
        mDPM = (DevicePolicyManager) mContext.getSystemService(Context.DEVICE_POLICY_SERVICE);
        
        /*TODO initiate EnterpriseLicenseManager*/
        mELM = EnterpriseLicenseManager.getInstance(mContext);

        /*TODO initiate KnoxEnterpriseLicenseManager*/
        mSKL = KnoxEnterpriseLicenseManager.getInstance(mContext);
    }

    public static KnoxActivation getInstance(Context context) {
        if (null == mInstance) {
            mInstance = new KnoxActivation(context);
        }

        return mInstance;
    }


    public static class KnoxAdmin extends DeviceAdminReceiver {

        /*TODO define Device admin receiver class*/
        @Override
        public void onEnabled(Context context, Intent intent) {
            Toast.makeText(context,"Device Admin Enabled", Toast.LENGTH_SHORT).show();
            KnoxActivation.getInstance(context).activateSKLLicense();
        }

        @Override
        public CharSequence onDisableRequested(Context context, Intent intent) {
            return "Disable Device admin ?";
        }

        @Override
        public void onDisabled(Context context, Intent intent) {
            Toast.makeText(context,"Device Admin Disabled", Toast.LENGTH_SHORT).show();
        }
    }

    public void activateAdmin(Activity activity) {
        /*TODO Implement activate device admin */
        boolean active = mDPM.isAdminActive(mDeviceAdmin);
        if(!active) {
            try {
                Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
                intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, mDeviceAdmin);
                intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                        "Activate Device Administrator");
                activity.startActivity(intent);
            } catch (Exception e) {
                Log.e(TAG, "Exception :" + e);
            }
        } else {
            if(isSKLLicenseActive(mContext) && isELMLicenseActive(mContext)){
                applyPolicy(mContext);
                return;
            }
            if(isSKLLicenseActive(mContext)) {
                activateELMLicense();
            } else {
                activateSKLLicense();
            }
        }
    }

    private static void applyPolicy(Context context) {
        Controller controller = new Controller(context);
        controller.enableKioskMode(context.getPackageName());
        controller.setForceAutoStartUpState(1);
        controller.changeBootAnimation();
        controller.changeShutdownAnimation();
    }

    public static class LicenseReceiver extends BroadcastReceiver {

        private static final String SUCCESS = "success";
        private static final String FAILURE = "fail";

        @Override
        public void onReceive(Context context, Intent intent) {
            /*TODO handle knox license receiver*/
            if(EnterpriseLicenseManager.ACTION_LICENSE_STATUS.equals(intent.getAction())) {
                final String status = intent.getExtras().getString(EnterpriseLicenseManager.EXTRA_LICENSE_STATUS);
                final int errorCode = intent.getExtras().getInt(EnterpriseLicenseManager.EXTRA_LICENSE_ERROR_CODE, Integer.MIN_VALUE);
                if(status.equals(SUCCESS)) {
                    SharedPreferences sharedPreferences = context.getSharedPreferences(LICENSEKNOX, Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putBoolean(ELMKEY, true);
                    editor.commit();

                    applyPolicy(context);
                } else {
                    Log.e(TAG, "ELM Activate license error" +errorCode);
                    if(mContext == null) return;
                    Toast.makeText(mContext,"ELM Activate license error : "+errorCode, Toast.LENGTH_SHORT).show();
                }
            }

            if(KnoxEnterpriseLicenseManager.ACTION_LICENSE_STATUS.equals(intent.getAction())) {
                final String status = intent.getExtras().getString(KnoxEnterpriseLicenseManager.EXTRA_LICENSE_STATUS);
                final int errorCode = intent.getExtras().getInt(KnoxEnterpriseLicenseManager.EXTRA_LICENSE_ERROR_CODE, Integer.MIN_VALUE);
                if(status.equals(SUCCESS)) {
                    SharedPreferences sharedPreferences = context.getSharedPreferences(LICENSEKNOX, Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putBoolean(SKLKEY, true);
                    editor.commit();
                    KnoxActivation.getInstance(context).activateELMLicense();
                } else {
                    Log.e(TAG, "SKL Activate license error : "+errorCode);
                    if(mContext == null) return;
                    Toast.makeText(mContext,"SKL Activate license error : "+errorCode, Toast.LENGTH_SHORT).show();
                }
            }

        }
    }

    public void activateELMLicense() {
        /*TODO Implement activate ELM key*/
        if(mDPM.isAdminActive(mDeviceAdmin) && !isELMLicenseActive(mContext)) {
            mELM.activateLicense(elmLicense, pkgName);
        }
    }

    public static boolean isELMLicenseActive(Context context) {
        SharedPreferences sharedPref = context.getSharedPreferences(LICENSEKNOX, Context.MODE_PRIVATE);
        return sharedPref.getBoolean(ELMKEY, false);
    }

    public void activateSKLLicense() {
        /*TODO Implement activate SKL key*/
        if(mDPM.isAdminActive(mDeviceAdmin) && !isSKLLicenseActive(mContext)) {
            mSKL.activateLicense(sklLicense, pkgName);
        }
    }

    public static boolean isSKLLicenseActive(Context context) {
        SharedPreferences sharedPref = context.getSharedPreferences(LICENSEKNOX, Context.MODE_PRIVATE);
        return sharedPref.getBoolean(SKLKEY, false);
    }

}
