package com.enchantedhunter.vmusic.ui.music;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import com.enchantedhunter.vmusic.common.LocalStorage;
import com.enchantedhunter.vmusic.data.Track;
import com.enchantedhunter.vmusic.ui.login.LoginActivity;
import com.enchantedhunter.vmusic.vkutils.VkUtils;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Environment;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import com.enchantedhunter.vmusic.R;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;

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

        Observable.fromCallable(new Callable<List<Track>>() {
            @Override
            public List<Track> call() throws Exception {
                return refreshMusicList();
            }
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<List<Track>>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(List<Track> trackList) {
                        MusicListAdapter adapter = new MusicListAdapter(MusicActivity.this ,trackList);
                        recyclerView.setAdapter(adapter);
                    }

                    @Override
                    public void onError(Throwable e) {
                        Toast.makeText(getApplicationContext(), "Error", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    public  boolean isStoragePermissionGranted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                Log.v("TAG","Permission is granted");
                return true;
            } else {

                Log.v("TAG","Permission is revoked");
                ActivityCompat.requestPermissions( MusicActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                return false;
            }
        }
        else { //permission is automatically granted on sdk<23 upon installation
            Log.v("TAG","Permission is granted");
            return true;
        }
    }


    private List<Track> refreshMusicList(){

        List<Track> tracks = new ArrayList<>();
        String token = null;
        try {
            token = LocalStorage.getDataFromFile(MusicActivity.this, LocalStorage.TOKEN_STORAGE);
            JsonElement resp = VkUtils.request("catalog.getAudio", token, new HashMap<String, String>(){{put("need_blocks", "1"); }});
            JsonArray audios = resp.getAsJsonObject().get("response").getAsJsonObject().get("audios").getAsJsonArray();

            isStoragePermissionGranted();

            for(int i = 0 ; i < audios.size() ; i ++){
                Track track = new Track(audios.get(i).getAsJsonObject());
                File folder = new File(Environment.getExternalStorageDirectory().toString() + "/VMUSIC/" + track.getId());

                if(folder.exists()){
                    File[] files =  folder.listFiles();
                    String fileName = String.format("%s-%s", track.getTitle(), track.getArtist());

                    for(File f : files){
                        if(f.getName().contains(fileName)){
                            track.progress = 100;
                            track.isLoaded = true;
                            break;
                        }
                    }
                }

                tracks.add(track);
            }
            return tracks;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}