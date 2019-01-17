package com.srin.kioskapp.view;

import android.app.Activity;
import android.content.res.AssetManager;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;

import com.srin.kioskapp.App;
import com.srin.kioskapp.R;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ScreenSaverActivity extends Activity implements View.OnTouchListener, MediaPlayer.OnCompletionListener,
        SurfaceHolder.Callback{

    public static final String VIDEO_DIR_PATH ="/Contents/";
    private static final long IMAGE_DURATION = 10 * 1000;
    private MediaPlayer mediaPlayer;
    private ArrayList<String> videoList = new ArrayList<String>();
    private SurfaceHolder holder;
    private int currentVideo = 0;
    private SurfaceView mSurfaceView;

    private List<String> videoExtList = Arrays.asList(new String[]{
            ".mp4", ".wmv", ".3gp", ".mkv", ".webm"
    });
    private List<String> imgExtList = Arrays.asList(new String[]{
            ".jpg", ".png"
    });


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE);
        setContentView(R.layout.surface_video_player);

        //load videos
        try{
            File file = new File(Environment.getExternalStorageDirectory(), VIDEO_DIR_PATH);

            for(File f : file.listFiles()){
                if(f.isDirectory()) continue;
                String path = f.getPath();
                String ext = getFileExtension(path);
                if(!videoExtList.contains(ext) && !imgExtList.contains(ext)) continue;
                videoList.add(path);
            }

        }catch (Exception e){
            e.printStackTrace();
            finish();
        }
        if(videoList.size()<1) finish();

        App.setVideoShowing(true);
        mSurfaceView = (SurfaceView) findViewById(R.id.surface);
        mSurfaceView.setOnTouchListener(this);
        holder = mSurfaceView.getHolder();
        holder.addCallback(this);
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        currentVideo = 0;
    }

    private static String getFileExtension(String path) {
        return path.substring(path.lastIndexOf(".")).toLowerCase();
    }

    @Override
    protected void onPause() {
        super.onPause();
        App.setVideoShowing(false);
        if(mediaPlayer==null) return;
        try{
            mediaPlayer.stop();
            mediaPlayer.release();
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height) {    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if(videoList.isEmpty()) return;
        String filePath = videoList.get(0);
        String ext = getFileExtension(filePath);
        if(videoExtList.contains(ext)){
            playVideo(filePath);
        }else if(imgExtList.contains(ext)){
            showImage(filePath);
        }
    }

    private void showImage(String imagepath){
        try{
            mSurfaceView.setBackground(Drawable.createFromPath(imagepath));
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    mSurfaceView.setBackground(null);
                    onCompletion(mediaPlayer);
                }
            }, IMAGE_DURATION);
        }catch (Exception e){
            e.printStackTrace();
            onCompletion(mediaPlayer);
        }

    }

    private void playVideo(String videoPath) {
        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setOnCompletionListener(this);
            mediaPlayer.setDisplay(holder);
            mediaPlayer.setDataSource(videoPath);
            mediaPlayer.prepare();
            mediaPlayer.start();
            App.setVideoShowing(true);
            return;
        } catch (IllegalArgumentException e) {
            Log.d("MEDIA_PLAYER", e.getMessage());
        } catch (IllegalStateException e) {
            Log.d("MEDIA_PLAYER", e.getMessage());
        } catch (IOException e) {
            Log.d("MEDIA_PLAYER", e.getMessage());
        }catch (Exception ee){
            Log.d("MEDIA PLAYER", ee.getMessage());
        }
        finish();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d("media player", "play next video");

    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        Log.d("player", "playback complete");
        currentVideo++;
        if (currentVideo > videoList.size() - 1) {
            currentVideo = 0;
        }
        try{
            if(mediaPlayer!=null) mediaPlayer.release();
            String filePath = videoList.get(currentVideo);
            String ext = getFileExtension(filePath);
            if(videoExtList.contains(ext)){
                playVideo(filePath);
            }else if(imgExtList.contains(ext)){
                showImage(filePath);
            }else{
                onCompletion(mediaPlayer);
            }
        }catch (Exception e){
            e.printStackTrace();
        }



    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        if(motionEvent.getAction()==MotionEvent.ACTION_DOWN){
            finish();
        }
        return true;
    }

    @Override
    protected void onStop() {
        super.onStop();
        App.setVideoShowing(false);
    }
}
