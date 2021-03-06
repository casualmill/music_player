package com.casualmill.musicplayer.activities;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.os.StrictMode;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.arlib.floatingsearchview.FloatingSearchView;
import com.casualmill.musicplayer.GlideApp;
import com.casualmill.musicplayer.MusicData;
import com.casualmill.musicplayer.MusicPlayer;
import com.casualmill.musicplayer.R;
import com.casualmill.musicplayer.adapters.MainPagerAdapter;
import com.casualmill.musicplayer.events.MusicServiceEvent;
import com.casualmill.musicplayer.fragments.QueueFragment;
import com.casualmill.musicplayer.models.Track;
import com.casualmill.musicplayer.utils.UserInterface;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/**
 * MainActivity.java
 * - Starting point of the application
 * - onCreate
 *      checks for permissions
 *          READ EXTERNAL STORAGE
 *      if has_permission:
 *          init()
 *              set activity_main.xml
 *              setup ViewPager, TabLayout, link buttons
 *
 */
public class MainActivity extends AppCompatActivity {

    public static final boolean DEVELOPER_MODE = true;

    // main_info collector
    private TextView track_title, track_author;
    private Button main_play_btn, sec_play_btn, queue_button;
    private ImageView main_albumArt, sec_albumArt;
    private SeekBar seekBar;
    private boolean update_ui = false, queue_fragment_shown = false;
    private Handler handler = new Handler();
    private Runnable mediaInfo_looper = new Runnable() {
        @Override
        public void run() {
            if (update_ui) {
                seekBar.setProgress(MusicPlayer.getProgress());
                handler.postDelayed(this, 100);
            }
        }
    };

    // BACK_KEY handler
    private SlidingUpPanelLayout slidingUpPanelLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
        super.onCreate(savedInstanceState);

        if (DEVELOPER_MODE) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build());

            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build());
        }

        // Permission has to be given by the user.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
        {
            Toast.makeText(this, "No Access Permissions", Toast.LENGTH_LONG).show();
            finish();
        } else
            init();
    }


    private void init() {
        setContentView(R.layout.activity_main);

        slidingUpPanelLayout = findViewById(R.id.slidingUpPanel);

        // toolbar
        // Toolbar toolBar = findViewById(R.id.toolbar);
        // setSupportActionBar(toolBar);

        // floatingSearchView
        FloatingSearchView floatingSearchView = findViewById(R.id.floating_search_view);
        DrawerLayout drawerLayoutl = findViewById(R.id.navigation_drawer);
        floatingSearchView.attachNavigationDrawerToMenuButton(drawerLayoutl);

        // setup ViewPager
        ViewPager pager = (ViewPager)findViewById(R.id.viewPager);
        MainPagerAdapter pagerAdapter = new MainPagerAdapter(this, getSupportFragmentManager());
        pager.setAdapter(pagerAdapter);

        //TabLayout
        TabLayout tabLayout = (TabLayout)findViewById(R.id.tabLayout);
        tabLayout.setupWithViewPager(pager);

        // Play Button
        View.OnClickListener playBtnListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MusicPlayer.playPause();
            }
        };
        main_play_btn = findViewById(R.id.button_play);
        main_play_btn.setOnClickListener(playBtnListener);
        sec_play_btn = findViewById(R.id.sec_play_button);
        sec_play_btn.setOnClickListener(playBtnListener);
        queue_button = findViewById(R.id.toggle_track_queue_button);
        queue_button.setVisibility(View.GONE);

        // Queue Fragment
        final View queue_fragment_view = findViewById(R.id.queue_fragment);
        queue_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // queue_fragment_view.animate().alpha(queue_fragment_shown ? 0 : 1).setDuration(500).start();
                UserInterface.animateViewVisibility(queue_fragment_view, queue_fragment_shown ? View.GONE : View.VISIBLE);
                queue_fragment_shown = !queue_fragment_shown;
            }
        });
        slidingUpPanelLayout.addPanelSlideListener(new SlidingUpPanelLayout.PanelSlideListener() {
            @Override
            public void onPanelSlide(View panel, float slideOffset) {
                sec_play_btn.setAlpha(1 - slideOffset);
                queue_button.setAlpha(slideOffset);
            }
            @Override public void onPanelStateChanged(View panel, SlidingUpPanelLayout.PanelState previousState, SlidingUpPanelLayout.PanelState newState) {
                if (newState == SlidingUpPanelLayout.PanelState.EXPANDED) {
                    // sec_play_btn.setAlpha(0); queue_button.setAlpha(1);
                    sec_play_btn.setVisibility(View.GONE);
                    queue_button.setVisibility(View.VISIBLE);
                } else if (newState == SlidingUpPanelLayout.PanelState.COLLAPSED) {
                    // sec_play_btn.setAlpha(1); queue_button.setAlpha(0);
                    sec_play_btn.setVisibility(View.VISIBLE);
                    queue_button.setVisibility(View.GONE);

                } else if (newState == SlidingUpPanelLayout.PanelState.DRAGGING)
                {
                    sec_play_btn.setVisibility(View.VISIBLE);
                    queue_button.setVisibility(View.VISIBLE);
                }
                queue_fragment_view.setVisibility(View.GONE);
            }
        });

        // TextViews
        track_title = findViewById(R.id.track_title);
        track_author = findViewById(R.id.track_author);

        // AlbumArts
        main_albumArt = findViewById(R.id.np_img_main);
        sec_albumArt = findViewById(R.id.np_img_sec);

        Button prevButton = findViewById(R.id.button_prev);
        prevButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MusicPlayer.playPrevious();
            }
        });

        Button nextButton = findViewById(R.id.button_next);
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MusicPlayer.playNext();
            }
        });

        // Seeker
        seekBar = findViewById(R.id.seekBar);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                // default [0,100]
                if (b) // user changed
                    MusicPlayer.seekToPosition(i);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { }
        });

        MusicPlayer.init(getApplicationContext());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMusicServiceEvent(MusicServiceEvent event) {

        // timed seekbar update only when is playing
        update_ui = event.eventType == MusicServiceEvent.EventType.PLAYING;
        if (update_ui) handler.post(mediaInfo_looper); // trigger the start

        // display pause icon only when playing
        main_play_btn.setBackgroundResource(event.eventType == MusicServiceEvent.EventType.PLAYING ? R.drawable.pause_filled : R.drawable.play_filled);
        sec_play_btn.setBackgroundResource(event.eventType == MusicServiceEvent.EventType.PLAYING ? R.drawable.pause : R.drawable.play);

        switch (event.eventType) {
            case INIT:
                // token for MediaController
                MusicPlayer.setToken(this, (MediaSessionCompat.Token) event.data);
                break;
            case COMPLETED:
            case STOPPED:
            case PREPARING:
                track_author.setText("");
                track_title.setText("");

                MusicData.setAlbumArt(this, -1, main_albumArt);
                MusicData.setAlbumArt(this, -1, sec_albumArt);
                break;
            case PLAYING:
                Track tr = (Track) event.data;
                track_author.setText(tr.artistName);
                track_title.setText(tr.title);
                MusicData.setAlbumArt(this, tr.albumId, main_albumArt);
                MusicData.setAlbumArt(this, tr.albumId, sec_albumArt);
                break;
            case PAUSED:
            case RESUMED:
                break;
        }
        if (event.eventType != MusicServiceEvent.EventType.COMPLETED) {

        } else {

        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (slidingUpPanelLayout.getPanelState() == SlidingUpPanelLayout.PanelState.EXPANDED) {
                slidingUpPanelLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onStart(){
        super.onStart();

        EventBus.getDefault().register(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }
}
