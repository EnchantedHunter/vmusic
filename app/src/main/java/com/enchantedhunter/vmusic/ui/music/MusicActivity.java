package com.enchantedhunter.vmusic.ui.music;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Bundle;

import com.enchantedhunter.vmusic.common.LocalStorage;
import com.enchantedhunter.vmusic.data.Track;
import com.enchantedhunter.vmusic.service.AudioService;
import com.enchantedhunter.vmusic.ui.login.LoginActivity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.enchantedhunter.vmusic.R;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MusicActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private MusicReceiver musicReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.getDefaultNightMode());
        getSupportActionBar().setTitle(getString(R.string.music_activity));

        recyclerView = (RecyclerView)findViewById(R.id.mrv);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true);

        initPlayerBottom();
        refreshPlaylist();
    }

    private void initPlayerBottom() {

    }

    public boolean isInternetAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnected();
    }

    public void refreshPlaylist(){
        if(isInternetAvailable()){
            loadPlaylist();
        }else {
            loadSavedPlaylist();
        }
    }

    public void loadSavedPlaylist(){

        ExecutorService executor = Executors.newSingleThreadExecutor();
        final Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(new Runnable() {

            @Override
            public void run() {

                File folder = new File(Environment.getExternalStorageDirectory().toString() + "/VMUSIC/");

                final List<Track> tracks = new ArrayList<>();

                if(folder.exists()) {
                    for (File tracksFold : folder.listFiles()) {
                        if(tracksFold.isDirectory()){
                            for (File trackFile: tracksFold.listFiles()) {
                                if(trackFile.isFile()){
                                    try{
                                        Track track = new Track();
                                        track.setSavedPath(trackFile.getAbsolutePath());
                                        track.setTitle(trackFile.getName().split("-")[0]);
                                        track.setArtist(trackFile.getName().split("-")[1]);
                                        track.isLoaded = true;
                                        track.progress = 100;
                                        track.setOwnerId("-1");
                                        track.setTrackId("-1");
                                        track.setUrl("");
                                        tracks.add(track);
                                    }catch (Exception e){
                                        Log.e("err", e.toString());
                                    }
                                }
                            }
                        }
                    }
                }

                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        recyclerView.setAdapter(new MusicListAdapter(MusicActivity.this, tracks));
                        Toast.makeText(getApplicationContext(), R.string.offline_mode, Toast.LENGTH_SHORT).show();

                        if(musicReceiver != null)
                            unregisterReceiver(musicReceiver);

                        musicReceiver = new MusicReceiver(((MusicListAdapter)recyclerView.getAdapter()).getTrackList());
                        IntentFilter intentFilter = new IntentFilter("vmusic");
                        registerReceiver(musicReceiver, intentFilter);

                    }
                });
            }
        });

    }

    public void loadPlaylist(){

        recyclerView.setAdapter(new MusicListAdapter(MusicActivity.this ,new ArrayList<Track>()));

        ExecutorService executor = Executors.newSingleThreadExecutor();
        final Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(new Runnable() {

            MusicRepository musicRepository;

            @Override
            public void run() {
                musicRepository = new MusicRepository();

                try {
                    final List<Track> tracks = musicRepository.refreshMusicList(MusicActivity.this);

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            MusicListAdapter musicListAdapter = (MusicListAdapter)recyclerView.getAdapter();
                            musicListAdapter.getTrackList().addAll(tracks);
                            musicListAdapter.notifyItemRangeInserted(musicListAdapter.getTrackList().size() - tracks.size(), musicListAdapter.getTrackList().size() + tracks.size());

                            if(musicReceiver != null)
                                unregisterReceiver(musicReceiver);

                            musicReceiver = new MusicReceiver(((MusicListAdapter)recyclerView.getAdapter()).getTrackList());
                            IntentFilter intentFilter = new IntentFilter("vmusic");
                            registerReceiver(musicReceiver, intentFilter);
                        }
                    });

                } catch (Exception e) {
                    Log.e("err", e.toString());
                }

                try {
                    List<Track> tracks = null;
                    while ((tracks = musicRepository.tryLoadNext()) != null){
                        final List<Track> finalTracks = tracks;
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                MusicListAdapter musicListAdapter = (MusicListAdapter)recyclerView.getAdapter();
                                musicListAdapter.getTrackList().addAll(finalTracks);
                                musicListAdapter.notifyItemRangeInserted(musicListAdapter.getTrackList().size() - finalTracks.size(), musicListAdapter.getTrackList().size() + finalTracks.size());

                                if(musicReceiver != null)
                                    unregisterReceiver(musicReceiver);

                                musicReceiver = new MusicReceiver(((MusicListAdapter)recyclerView.getAdapter()).getTrackList());
                                IntentFilter intentFilter = new IntentFilter("vmusic");
                                registerReceiver(musicReceiver, intentFilter);
                            }
                        });
                    }
                } catch (Exception e) {
                    Log.e("err", e.toString());
                }
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_exit:

                try {
                    LocalStorage.setDataInFile(MusicActivity.this, LocalStorage.TOKEN_STORAGE, null);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                this.startService(new Intent(this, AudioService.class)
                        .putExtra(AudioService.SERVICE_ACTION, AudioService.ACTION.STOP.name()));

                this.finish();
                startActivity(new Intent(this, LoginActivity.class));

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}