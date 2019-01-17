package com.srin.securenotes;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.MimeTypeMap;
import android.widget.EditText;
import android.widget.Toast;

import com.samsung.android.knox.sdp.SdpFileSystem;
import com.samsung.android.knox.sdp.core.SdpException;
import com.samsung.android.sdk.SsdkUnsupportedException;
import com.samsung.android.sdk.pass.Spass;
import com.samsung.android.sdk.pass.SpassFingerprint;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import com.srin.securenotes.R;

public class MainActivity extends AppCompatActivity {

    private final static String TAG = "DEX_NOTEPAD";

    public static final String SDP_ALIAS = "secure_note_alias";
    public static final String SDP_PASSWORD = "sdp2018";

    private EditText editTextView;
    private Spass spass;
    private SpassFingerprint spassFingerprint;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //...called when the activity is starting. This is where most initialization should go
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        editTextView = (EditText) findViewById(R.id.edt);

        // Initialize Spass
        spass = new Spass();

        try {
            spass.initialize(this);
            spassFingerprint = new SpassFingerprint(this);
        } catch (SsdkUnsupportedException e) {
            Toast.makeText(this, "Device is not supported for fingerprint", Toast.LENGTH_SHORT).show();
        }

        createSDPEngine();
    }

    private void checkFingerprint () {
        if(spassFingerprint!=null && !spassFingerprint.hasRegisteredFinger()) {
            Toast.makeText(this, "Please Register Fingerprint to use Memo Pad!", Toast.LENGTH_LONG).show();
            spassFingerprint.registerFinger(MainActivity.this, registerListener);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_identify:
                if(spassFingerprint==null)
                    Toast.makeText(this, "Device is not supported for fingerprint", Toast.LENGTH_SHORT).show();

                if(spassFingerprint!=null && spassFingerprint.hasRegisteredFinger()) {
                    spassFingerprint.startIdentifyWithDialog(MainActivity.this, identifyListener, true);
                } else {
                    editTextView.setText(loadFile());
                }
                 return true;

            case R.id.menu_use_fingerprint:
              checkFingerprint();
                return true;
            case R.id.menu_save:
                saveFile();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void createSDPEngine() {
        //TODO: Create SDP Engine
    }

    /*
     * Reads contents of a file into the editable area
     */
    private String loadFile() {
        //TODO: Load file from SDP
        return "";
    }

    /*
     * Saves the contents of the editable are into a sensitive data file
     */
    private void saveFile() {
        //TODO: Save file to SDP

    }


    @Override
    protected void onPause() {
        // ...this method is used to store global persistent data (in content providers, files, etc.)
        super.onPause();
        // DeX handle runtime configuration change TODO Save the entered text
        saveFile();
    }

    SpassFingerprint.RegisterListener registerListener = new SpassFingerprint.RegisterListener() {
        @Override
        public void onFinished() {
//            Toast.makeText(mContext, "Register fingerprint finished", Toast.LENGTH_SHORT).show();
        }
    };

    SpassFingerprint.IdentifyListener identifyListener = new SpassFingerprint.IdentifyListener() {
        @Override
        public void onFinished(int i) {
            if(SpassFingerprint.STATUS_AUTHENTIFICATION_SUCCESS == i){
                editTextView.setText(loadFile());
            }else{
                Log.d(TAG, "Fingerprint not found!");
            }
        }

        @Override
        public void onReady() {

        }

        @Override
        public void onStarted() {

        }

        @Override
        public void onCompleted() {

        }
    };

}
