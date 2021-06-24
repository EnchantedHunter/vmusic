package com.enchantedhunter.vmusic.ui.music;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;

import com.enchantedhunter.vmusic.common.LocalStorage;
import com.enchantedhunter.vmusic.data.Track;
import com.enchantedhunter.vmusic.service.AudioService;
import com.enchantedhunter.vmusic.ui.login.LoginActivity;
import com.enchantedhunter.vmusic.vkutils.VkUtils;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
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
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class MusicActivity extends AppCompatActivity {

    private RecyclerView recyclerView;

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
                            }
                        });
                    }
                } catch (Exception e) {
                    Log.e("err", e.toString());
                }

            }
        });


//        Observable.fromCallable(new Callable<List<Track>>() {
//            @Override
//            public List<Track> call() throws Exception {
//                return refreshMusicList();
//            }
//        })
//                .subscribeOn(Schedulers.io())
//                .observeOn(AndroidSchedulers.mainThread())
//                .subscribe(new Observer<List<Track>>() {
//                    @Override
//                    public void onSubscribe(Disposable d) {
//
//                    }
//
//                    @Override
//                    public void onNext(List<Track> trackList) {
//                        MusicListAdapter adapter = new MusicListAdapter(MusicActivity.this ,trackList);
//                        recyclerView.setAdapter(adapter);
//                    }
//
//                    @Override
//                    public void onError(Throwable e) {
//                        Toast.makeText(getApplicationContext(), "Ошибка", Toast.LENGTH_SHORT).show();
//                    }
//
//                    @Override
//                    public void onComplete() {
//
//                    }
//                });

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

    public  boolean isStoragePermissionGranted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                return true;
            } else {
                ActivityCompat.requestPermissions( MusicActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                return false;
            }
        }
        else {
            return true;
        }
    }
}