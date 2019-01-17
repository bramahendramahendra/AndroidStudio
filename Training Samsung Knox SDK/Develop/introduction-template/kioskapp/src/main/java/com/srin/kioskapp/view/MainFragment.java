package com.srin.kioskapp.view;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.AdaptiveVideoTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.ui.PlaybackControlView;
import com.google.android.exoplayer2.ui.SimpleExoPlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import com.srin.kioskapp.R;
import com.srin.kioskapp.controller.Controller;
import com.srin.kioskapp.controller.KnoxActivation;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by SRIN on 1/4/2017.
 */
public class MainFragment extends Fragment implements PlaybackControlView.VisibilityListener, View.OnClickListener, ExoPlayer.EventListener{

    private final static int PERMISSION_REQUEST_CODE  = 0x1f;

    private TrackSelection.Factory videoTrackSelectionFactory;
    private TrackSelector trackSelector;
    private LoadControl loadControl;
    private SimpleExoPlayer player;
    private SimpleExoPlayerView simpleExoPlayerView;
    private Controller mController;

    private Playlist playlist;
    private RecyclerView playlistRecyclerView;
    private PlaylistAdapter playlistAdapter;

    private ImageButton mPrevButton;
    private ImageButton mNextButton;

    private TextView mFileTitle;
    private TextView mPlayerStatus;

    private Button mExitKiosk;

    private int mCurrentPos;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.main_fragment_temp, container, false);

        requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);

        KnoxActivation.getInstance(getActivity()).activateAdmin(getActivity());

        mController = Controller.getInstance(getContext());

        mExitKiosk = (Button) rootView.findViewById(R.id.exit_kiosk);
        mExitKiosk.setOnClickListener(this);

        mPrevButton = (ImageButton) rootView.findViewById(R.id.prev_btn);
        mNextButton = (ImageButton) rootView.findViewById(R.id.next_btn);

        mPrevButton.setOnClickListener(this);
        mNextButton.setOnClickListener(this);

        mCurrentPos = 0;
        checkState();

        mFileTitle = (TextView) rootView.findViewById(R.id.file_title);
        mPlayerStatus = (TextView) rootView.findViewById(R.id.player_status);

        simpleExoPlayerView = (SimpleExoPlayerView) rootView.findViewById(R.id.player_view);
        simpleExoPlayerView.setControllerVisibilityListener(this);
        simpleExoPlayerView.requestFocus();
        simpleExoPlayerView.setControllerShowTimeoutMs(0);
        simpleExoPlayerView.setKeepScreenOn(true);
        creatingPlayer();

        playlistRecyclerView = (RecyclerView) rootView.findViewById(R.id.playlist_view);
        playlistRecyclerView.setHasFixedSize(true);
        playlistRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        playlist = new Playlist();
        playlistAdapter = new PlaylistAdapter();
        playlistRecyclerView.setAdapter(playlistAdapter);

        return rootView;
    }

    public void creatingPlayer() {
        // 1. Create a default TrackSelector
        videoTrackSelectionFactory = new AdaptiveVideoTrackSelection.Factory(null);
        trackSelector = new DefaultTrackSelector(videoTrackSelectionFactory);
        // 2. Create a default LoadControl
        loadControl = new DefaultLoadControl();
        // 3. Create the player
        player = ExoPlayerFactory.newSimpleInstance(getActivity(), trackSelector, loadControl);
        player.addListener(this);
        simpleExoPlayerView.setPlayer(player);
    }

    public void preparingPlayer(Uri uri) {
        if(player != null) {
            player.setPlayWhenReady(false);
            player.stop();
            player.seekTo(0);
        }
        // Produces DataSource instances through which media data is loaded.
        DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(getActivity(), Util.getUserAgent(getActivity(), "ToyotaMediaPlayer"), null);
        // Produces Extractor instances for parsing the media data.
        ExtractorsFactory extractorsFactory = new DefaultExtractorsFactory();
        // This is the MediaSource representing the media to be played.
        MediaSource videoSource = new ExtractorMediaSource( uri, dataSourceFactory, extractorsFactory, null, null);
        // Prepare the player with the source.
        player.prepare(videoSource);
        player.setPlayWhenReady(true);
    }


    @Override
    public void onVisibilityChange(int visibility) {
        if (visibility == PlaybackControlView.GONE) {
            simpleExoPlayerView.showController();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.prev_btn:
                setButtonActive(mCurrentPos, false);
                playFileAtPos(--mCurrentPos);
                break;
            case R.id.next_btn:
                setButtonActive(mCurrentPos, false);
                playFileAtPos(++mCurrentPos);
                break;
            case R.id.exit_kiosk:
                mController.disableKioskMode();
                break;
        }
    }

    @Override
    public void onTimelineChanged(Timeline timeline, Object o) {

    }

    @Override
    public void onTracksChanged(TrackGroupArray trackGroupArray, TrackSelectionArray trackSelectionArray) {

    }

    @Override
    public void onLoadingChanged(boolean b) {

    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int state) {
        if (!playWhenReady) {
            mPlayerStatus.setText("Paused");
        } else {
            if (state == ExoPlayer.STATE_ENDED && mCurrentPos != playlistAdapter.getItemCount() - 1) {
                setButtonActive(mCurrentPos, false);
                playFileAtPos(++mCurrentPos);
            }

            mPlayerStatus.setText("Now Playing..");
        }

    }

    @Override
    public void onPlayerError(ExoPlaybackException e) {

    }

    @Override
    public void onPositionDiscontinuity() {

    }

    class PlaylistAdapter extends RecyclerView.Adapter<PlaylistAdapter.ViewHolder> {
        @Override
        public PlaylistAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.playlist_itemview, parent, false);
            return new PlaylistAdapter.ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(PlaylistAdapter.ViewHolder holder, int position) {
            MediaMetadataRetriever metadataRetriever = new MediaMetadataRetriever();
            metadataRetriever.setDataSource(playlist.getList().get(position).getPath());

            // set title
            String title = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
            if (title == null || title.length() == 0) {
                title = playlist.getList().get(position).getName();
            }
            holder.titleMedia.setText(title);

            // set icon
            if (metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO) != null) {
                holder.imageMedia.setImageResource(R.drawable.video_icon);
            } else if (metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO) != null){
                holder.imageMedia.setImageResource(R.drawable.music_icon);
            }
        }

        @Override
        public int getItemCount() {
            return playlist == null ? 0 : playlist.size();
        }

        public String getItemTitleAt (int index) {
            if (playlist != null && playlist.size() > 0 && playlist.size() > index) {
                return playlist.getList().get(index).getName();
            }

            return "";
        }

        class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
            public ImageView imageMedia;
            public TextView titleMedia;

            public ViewHolder(View itemView) {
                super(itemView);
                imageMedia = (ImageView) itemView.findViewById(R.id.media_image);
                titleMedia = (TextView) itemView.findViewById(R.id.media_title);
                itemView.setOnClickListener(this);
            }

            @Override
            public void onClick(View view) {
                setButtonActive(mCurrentPos, false);
                mCurrentPos = getAdapterPosition();
                playFileAtPos(mCurrentPos);
            }
        }
    }

    public void openPlaylistFolder(File playlistFolder) {
        playlist.open(playlistFolder);
        playlistAdapter.notifyDataSetChanged();
    }

    public void setButtonActive (int position, boolean state) {
        if(playlistRecyclerView.getChildAt(position)==null) return;
        View view = playlistRecyclerView.getChildAt(position).findViewById(R.id.list_bg);
        if (state) {
            view.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.list_bg_active));
        } else {
            view.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.list_bg));
        }
    }

    public void playFileAtPos (int position) {
        mFileTitle.setText(playlistAdapter.getItemTitleAt(position));
        setButtonActive(position, true);
        checkState();
        if(playlist.getList()!=null && !playlist.getList().isEmpty()
                && playlist.getList().size() > position){
            Uri data = Uri.fromFile(playlist.getList().get(position));
            preparingPlayer(data);
        }

    }

    public void checkState () {
        if (mCurrentPos == 0) {
            mPrevButton.setEnabled(false);
            mNextButton.setEnabled(true);
        } else if (mCurrentPos == playlistAdapter.getItemCount() - 1) {
            mPrevButton.setEnabled(true);
            mNextButton.setEnabled(false);
        } else {
            mPrevButton.setEnabled(true);
            mNextButton.setEnabled(true);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (PERMISSION_REQUEST_CODE == requestCode
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openPlaylistFolder(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC));
            openPlaylistFolder(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES));
            new Controller(getContext()).copyAssets("");
            new Controller(getContext()).copyAssets("screensaver");
        } else {
            Toast.makeText(getActivity(), "Read External Storage Permission Denied", Toast.LENGTH_SHORT).show();
        }
    }

    class Playlist {
        private List<File> playlistFiles =  new ArrayList<>();

        public void open(File folder) {
            MediaMetadataRetriever metadataRetriever = new MediaMetadataRetriever();
            if( folder.listFiles()==null) return;
            // filter audio or video file only
            for (File file: folder.listFiles()) {
                metadataRetriever.setDataSource(file.getPath());
                if (metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO) != null ||
                        metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO) != null) {
                    playlistFiles.add(file);
                }
            }
        }

        public int size() {
            return playlistFiles.size();
        }

        public List<File> getList() {
            return playlistFiles;
        }
    }

    @Override
    public void onDestroy() {
        player.release();
        super.onDestroy();
    }



}
