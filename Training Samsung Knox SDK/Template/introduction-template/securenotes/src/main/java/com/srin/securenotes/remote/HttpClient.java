package com.srin.securenotes.remote;

import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import java.io.IOException;

/**
 * @author BIL-ID
 */
public class HttpClient {

    public static final MediaType JSON = MediaType
            .parse("application/json; charset=utf-8");

    private static HttpClient mInstance;
    private OkHttpClient mOk;

    public HttpClient() {
        mOk = new OkHttpClient();
    }

    public static HttpClient getInstance() {
        if (mInstance == null) {
            mInstance = new HttpClient();
        }
        return mInstance;
    }

    /**
     * request nonce from MDM server
     *
     * @param url
     * @param apiKey
     * @return
     * @throws IOException
     */
    public String getNonce(String url, String apiKey) throws IOException {
        String post = "";
        RequestBody body = RequestBody.create(JSON, post);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("x-knox-attest-api-key", apiKey)
                .addHeader("Accept", "application/json")
                .build();
        Response response = mOk.newCall(request).execute();
        return response.body().string();
    }

    /**
     * request status from MDM server
     *
     * @param url
     * @param blob
     * @param apiKey
     * @return
     * @throws IOException
     */
    public String getAttestationStatus(String url, byte[] blob, String apiKey) throws IOException {
        RequestBody body = RequestBody.create(MediaType.parse("application/octet-stream"), blob);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("x-knox-attest-api-key", apiKey)
                .addHeader("Accept", "application/json")
                .addHeader("Content-type", "application/octet-stream")
                .build();
        Response response = mOk.newCall(request).execute();
        return response.body().string();
    }
}
