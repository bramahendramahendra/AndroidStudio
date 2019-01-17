package com.srin.securenotes.controller;

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

import com.samsung.android.knox.EnterpriseDeviceManager;
import com.samsung.android.knox.license.EnterpriseLicenseManager;
import com.samsung.android.knox.license.KnoxEnterpriseLicenseManager;
import com.samsung.android.knox.integrity.AttestationPolicy;
import com.srin.securenotes.AttestationActivity;
import com.srin.securenotes.MainActivity;

public class PolicyController {

    private static PolicyController mInstance;
    private static Context mContext;
    private Toast mToast;
    private ComponentName mDeviceAdmin;
    private DevicePolicyManager mDPM;
    private EnterpriseLicenseManager mELM;
    private KnoxEnterpriseLicenseManager mSKL;
    private EnterpriseDeviceManager mEDM;

    public static String LICENSEKNOX = "LICENSEKNOX";
    public static String ELMKEY = "ELMKEY";
    public static String SKLKEY = "SKLKEY";


    private static String TAG = PolicyController.class.getSimpleName();
    private String elmLicense = "94E51A2A93A6C626D5CE856857FE18DDAD6547A11B44054DAF9075AC55287A8818699A4538100DBA203C10F862E223E010DB500721A441F148C9B8DE72DCE557";// Please use Backwards-compatible key
    private String sklLicense = "KLM06-6C206-SOEWP-ZSI0A-YPNZO-AU462";
    public static String attestationAPIKey = "";

    private String pkgName = "";
    private static String mNonce = "";

    public PolicyController(Context context) {
        mContext = context;
        mToast = Toast.makeText(mContext, "", Toast.LENGTH_SHORT);
        mDeviceAdmin = new ComponentName(mContext, KnoxAdmin.class);
        mDPM = (DevicePolicyManager) mContext.getSystemService(Context.DEVICE_POLICY_SERVICE);
        mELM = EnterpriseLicenseManager.getInstance(mContext);
        mSKL = KnoxEnterpriseLicenseManager.getInstance(mContext);
        mEDM = EnterpriseDeviceManager.getInstance(mContext);
        pkgName = context.getPackageName();
    }

    public static PolicyController getInstance(Context context) {
        if (null == mInstance) {
            mInstance = new PolicyController(context);
        }

        return mInstance;
    }

    public static class KnoxAdmin extends DeviceAdminReceiver {
        @Override
        public void onEnabled(Context context, Intent intent) {
            Toast.makeText(context,"Device Admin Enabled", Toast.LENGTH_SHORT).show();
            PolicyController.getInstance(context).activateSKLLicense();
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
        boolean active = mDPM.isAdminActive(mDeviceAdmin);
        if(!active) {
            try {
                Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
                intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, mDeviceAdmin);
                intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                        "Activate Device Administrator");
                ((AttestationActivity)activity).updateProgressStatus("Activating Device Admin & KNOX license...\n");
                activity.startActivity(intent);
            } catch (Exception e) {
                Log.e(TAG, "Exception :" + e);
            }
        } else {
            ((AttestationActivity)activity).updateProgressStatus("Device admin is already activated!\n");
            ((AttestationActivity)activity).updateProgressStatus("Activating KNOX license...\n");
            if(isSKLLicenseActive(mContext)) {
                activateELMLicense();
            } else {
                activateSKLLicense();
            }
        }
    }

    public static class LicenseReceiver extends BroadcastReceiver {

        private static final String SUCCESS = "success";
        private static final String FAILURE = "fail";

        private boolean mELMActive = false;
        private boolean mKLMActive = false;

        static AttestationActivity activity;

        public LicenseReceiver() {

        }

        // Get the current fragment instance
        public LicenseReceiver(AttestationActivity activity) {
            AttestationReceiver.activity = activity;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if(KnoxEnterpriseLicenseManager.ACTION_LICENSE_STATUS.equals(intent.getAction())) {
                final String status = intent.getExtras().getString(KnoxEnterpriseLicenseManager.EXTRA_LICENSE_STATUS);
                final int errorCode = intent.getExtras().getInt(KnoxEnterpriseLicenseManager.EXTRA_LICENSE_ERROR_CODE, Integer.MIN_VALUE);
                if(status.equals(SUCCESS)) {
                    SharedPreferences sharedPreferences = context.getSharedPreferences(LICENSEKNOX, Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putBoolean(SKLKEY, true);
                    editor.commit();
                    PolicyController.getInstance(context).activateELMLicense();
                } else {
                    Toast.makeText(mContext,"SKL Activate license error : "+errorCode, Toast.LENGTH_SHORT).show();
                }
            } else if(EnterpriseLicenseManager.ACTION_LICENSE_STATUS.equals(intent.getAction())) {
                final String status = intent.getExtras().getString(EnterpriseLicenseManager.EXTRA_LICENSE_STATUS);
                final int errorCode = intent.getExtras().getInt(EnterpriseLicenseManager.EXTRA_LICENSE_ERROR_CODE, Integer.MIN_VALUE);
                if(status.equals(SUCCESS)) {
                    SharedPreferences sharedPreferences = context.getSharedPreferences(LICENSEKNOX, Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putBoolean(ELMKEY, true);
                    editor.commit();
                    ((AttestationActivity)mContext).startAttestation();
                } else {
                    Toast.makeText(mContext,"ELM Activate license error : "+errorCode, Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    public static class AttestationReceiver extends BroadcastReceiver {
        private static final String TAG = "Attestation";
        static AttestationActivity activity;
        public AttestationReceiver() { }
        // Get the current fragment instance
        public AttestationReceiver(AttestationActivity activity) {
            AttestationReceiver.activity = activity;
        }
        @Override
        public void onReceive(Context context, Intent intent) {
            if (AttestationPolicy.ACTION_KNOX_ATTESTATION_RESULT.equals(intent
                    .getAction())) {
                final int result = intent.getIntExtra(AttestationPolicy.EXTRA_RESULT,
                        Integer.MIN_VALUE);
                final String errorMsg = intent.getExtras().getString(
                        AttestationPolicy.EXTRA_ERROR_MSG);
                switch (result) {
                    //The communication with the TrustZone and the Attestation server was successful
                    case AttestationPolicy.ERROR_NONE:
                        byte[] blob = intent.getByteArrayExtra(AttestationPolicy.EXTRA_ATTESTATION_DATA);
                        activity.updateProgressStatus("The communication with the TrustZone and the Attestation server was successful");
                        activity.updateProgressStatus("Get blob from attestation complete...");
                        activity.requestAttestationStatus(mNonce, blob);
                        activity.updateProgressStatus("Start get attestation status process...");
                        break;
                    //Device is not compatible for attestation
                    case AttestationPolicy.ERROR_DEVICE_NOT_SUPPORTED:
                        if (errorMsg != null) {
                            Log.w(TAG, errorMsg);
                        }
                        activity.updateProgressStatus("Device is not compatible for attestation");
                        break;
                    //invalid nonce provided.
                    case AttestationPolicy.ERROR_INVALID_NONCE:
                        activity.updateProgressStatus("Invalid nonce provided");
                        break;
                    //Only MDM partner client with appropriate permission can invoke thisAPI
                    case AttestationPolicy.ERROR_MDM_PERMISSION:
                        activity.updateProgressStatus("Only MDM partner client with appropriate permission can invoke this API");
                        break;
                    //Error communicating with the trust zone
                    case AttestationPolicy.ERROR_TIMA_INTERNAL:
                        activity.updateProgressStatus("Error communicating with the trust zone");
                        break;
                    //Unknown error during attestation process
                    case AttestationPolicy.ERROR_UNKNOWN:
                        if (errorMsg != null) {
                            Log.w(TAG, errorMsg);
                        }
                        activity.updateProgressStatus("Unknown error during attestation process");
                        break;
                    default:
                        activity.updateProgressStatus("Unknown error during attestation process");
                        break;
                }
            }
        }
    }

    public void activateELMLicense() {
        if(mDPM.isAdminActive(mDeviceAdmin) && !isELMLicenseActive(mContext)) {
            mELM.activateLicense(elmLicense, pkgName);
        } else {
            Toast.makeText(mContext, "ELM and SKL has been activated!", Toast.LENGTH_SHORT).show();
            ((AttestationActivity)mContext).startAttestation();
        }
    }

    public static boolean isELMLicenseActive(Context context) {
        SharedPreferences sharedPref = context.getSharedPreferences(LICENSEKNOX, Context.MODE_PRIVATE);
        boolean elmKey = sharedPref.getBoolean(ELMKEY, false);
        return elmKey;
    }

    public void activateSKLLicense() {
        if(mDPM.isAdminActive(mDeviceAdmin) && !isSKLLicenseActive(mContext)) {
            mSKL.activateLicense(sklLicense, pkgName);
        }
    }

    public static boolean isSKLLicenseActive(Context context) {
        SharedPreferences sharedPref = context.getSharedPreferences(LICENSEKNOX, Context.MODE_PRIVATE);
        boolean klmKey = sharedPref.getBoolean(SKLKEY, false);
        return klmKey;
    }

    public void startAttestation (String nonce) {
        mNonce = nonce;
        AttestationPolicy attestation = new AttestationPolicy(mContext);
        try {
            attestation.startAttestation(nonce);
        } catch (SecurityException e) {
            Log.w(TAG, "SecurityException: " + e);
        }
    }
}
