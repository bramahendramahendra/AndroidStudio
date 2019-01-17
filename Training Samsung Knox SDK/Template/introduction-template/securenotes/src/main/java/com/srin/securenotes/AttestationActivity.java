package com.srin.securenotes;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.srin.securenotes.controller.PolicyController;
import com.srin.securenotes.remote.HttpClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class AttestationActivity extends AppCompatActivity implements View.OnClickListener {

    private String mNonce;
    private String mAttestationStatus;
    private StringBuffer mText;
    private Button mBtnStartApplication;
    private TextView mTxtAttestationStatus;
    public ProgressDialog dialog;

    private static final String TAG = "SECURE_MEMO";
    private static final String URL_MDM_SERVER_NONCE = "http://bil-id.com/attestation/nonces";
    private static final String URL_MDM_SERVER_MEASUREMENT = "http://bil-id.com/attestation/measurements";

    private GetNonceTask getNonceTask;
    private GetMeasurementTask getMeasurementTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attestation);
        mBtnStartApplication = (Button) findViewById(R.id.btnStartApplication);
        mBtnStartApplication.setOnClickListener(this);
        mTxtAttestationStatus = (TextView) findViewById(R.id.txtAttestationStatus);

        new PolicyController.AttestationReceiver(this);
        new PolicyController.LicenseReceiver(this);

        requestPermissions(new String[]{Manifest.permission.INTERNET}, 0);

        mTxtAttestationStatus.setText("");
        PolicyController.getInstance(this).activateAdmin(this);

    }

    @Override
    protected void onDestroy() {
        if (getNonceTask != null) {
            getNonceTask.setListener(null);
        }
        if (getMeasurementTask != null) {
            getMeasurementTask.setListener(null);
        }
        super.onDestroy();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btnStartApplication:
                Intent intent = new Intent(AttestationActivity.this, MainActivity.class);
                AttestationActivity.this.startActivity(intent);
                finish();
                break;
        }
    }

    public static class GetNonceTask extends AsyncTask<String, String, String> {
        private NonceTaskListener listener;

        @Override
        protected String doInBackground(String... strings) {
            String response = null;
            //TODO: Request nonce from server
            return response;
        }

        @Override
        protected void onPostExecute(String s) {
            listener.onNonceTaskPostkExecute(s);
        }

        @Override
        protected void onProgressUpdate(String... values) {
            listener.onNonceTaskProgressUpdate(values[0]);
        }

        public void setListener(NonceTaskListener listener) {
            this.listener = listener;
        }

        public interface NonceTaskListener {
            void onNonceTaskPostkExecute(String s);
            void onNonceTaskProgressUpdate(String values);
        }
    }

    public static class Data {
        public String nonce;
        public byte[] blob;
    }

    public static class GetMeasurementTask extends AsyncTask<Data, String, String> {

        private MeasurementTaskListener listener;

        @Override
        protected String doInBackground(Data... data) {
            String response = null;
            try {
                response = HttpClient.getInstance()
                        .getAttestationStatus(URL_MDM_SERVER_MEASUREMENT + "?nonce=" + data[0].nonce,
                                data[0].blob, PolicyController.attestationAPIKey);
            } catch (IOException e) {
                //Handle IOException when get attestation status response from MDM server
                e.printStackTrace();
            }
            return response;
        }

        @Override
        protected void onPostExecute(String s) {
            listener.onMeasurementTaskPostkExecute(s);
        }

        @Override
        protected void onProgressUpdate(String... values) {
            listener.onMeasurementTaskProgressUpdate(values[0]);
        }

        public void setListener(MeasurementTaskListener listener) {
            this.listener = listener;
        }

        public interface MeasurementTaskListener {
            void onMeasurementTaskPostkExecute(String s);
            void onMeasurementTaskProgressUpdate(String values);
        }
    }

    public void startAttestation() {
        mAttestationStatus = "";
        mNonce = "";
        updateProgressStatus("Request to get nonce from MDM Server...");
        //Use AsyncTask to prevent blocking when calling getNonce using synchronous HTTP request
        getNonceTask = new GetNonceTask();
        getNonceTask.setListener(new GetNonceTask.NonceTaskListener() {
            @Override
            public void onNonceTaskPostkExecute(String response) {
                //TODO: Start Attestation from received nonce
            }

            @Override
            public void onNonceTaskProgressUpdate(String values) {
                updateProgressStatus(values);
            }
        });
        getNonceTask.execute(null, null);
    }

    /**
     * task for get attestation status from MDM server after get blob
     *
     * @param blob
     */
    public void requestAttestationStatus(final String nonce, final byte[] blob) {
        String response;
        //Use AsyncTask to prevent blocking when calling getAttestationStatus using synchronous HTTP request
        getMeasurementTask = new GetMeasurementTask();
        getMeasurementTask.setListener(new GetMeasurementTask.MeasurementTaskListener() {
            @Override
            public void onMeasurementTaskPostkExecute(String response) {
                //TODO: Read measurement to identify device's integrity
            }

            @Override
            public void onMeasurementTaskProgressUpdate(String values) {
                updateProgressStatus(values);
            }
        });

        Data data = new Data();
        data.nonce = nonce;
        data.blob = blob;
        getMeasurementTask.execute(data);
    }

    public void updateProgressStatus(String status) {
        if (mTxtAttestationStatus != null) {
            mTxtAttestationStatus.append(status + "\n");
        }
    }

    /*
     * Displays a progress dialog
     */
    public void showProgress(String msg) {
        dialog = new ProgressDialog(this);
        dialog.setMessage(msg);
        dialog.setIndeterminate(true);
        dialog.setCancelable(false);
        dialog.show();
    }

    /*
     * Hides the progress dialog
     */
    public void hideProgress() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }
}
