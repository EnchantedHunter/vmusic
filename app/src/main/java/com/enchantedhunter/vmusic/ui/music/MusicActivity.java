package com.enchantedhunter.vmusic.ui.music;

import android.os.Bundle;

import com.enchantedhunter.vmusic.common.LocalStorage;
import com.enchantedhunter.vmusic.data.Track;
import com.enchantedhunter.vmusic.ui.login.LoginActivity;
import com.enchantedhunter.vmusic.vkutils.VkUtils;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import com.enchantedhunter.vmusic.R;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

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

        recyclerView = (RecyclerView)findViewById(R.id.mrv);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true);

        Observable.fromCallable(new Callable<List<Track>>() {
            @Override
            public List<Track> call() throws Exception {
                return refreshMusicList();
            }
        }) // Execute in IO thread, i.e. background thread.
                .subscribeOn(Schedulers.io())
                // report or post the result to main thread.
                .observeOn(AndroidSchedulers.mainThread())
                // execute this RxJava
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

    private List<Track> refreshMusicList(){

        List<Track> tracks = new ArrayList<>();
        String token = null;
        try {
            token = LocalStorage.getDataFromFile(MusicActivity.this, LocalStorage.TOKEN_STORAGE);
            JsonElement resp = VkUtils.request("catalog.getAudio", token, new HashMap<String, String>(){{put("need_blocks", "1"); }});
            JsonArray audios = resp.getAsJsonObject().get("response").getAsJsonObject().get("audios").getAsJsonArray();

            for(int i = 0 ; i < audios.size() ; i ++){
                tracks.add(new Track(audios.get(i).getAsJsonObject()));
            }
            return tracks;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}