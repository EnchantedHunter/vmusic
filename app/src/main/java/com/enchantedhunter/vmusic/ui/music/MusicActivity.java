package com.enchantedhunter.vmusic.ui.music;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
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
                        Toast.makeText(getApplicationContext(), "Ошибка", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onComplete() {

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

    private List<Track> refreshMusicList(){

        List<Track> tracks = new ArrayList<>();
        String token = null;
        try {

            token = LocalStorage.getDataFromFile(MusicActivity.this, LocalStorage.TOKEN_STORAGE);
            JsonElement resp = VkUtils.request("catalog.getAudio", token, new HashMap<String, String>(){{put("need_blocks", "1");}});

            JsonArray sections = resp.getAsJsonObject().get("response").getAsJsonObject().get("catalog").getAsJsonObject().get("sections").getAsJsonArray();
            JsonArray audios = resp.getAsJsonObject().get("response").getAsJsonObject().get("audios").getAsJsonArray();

            String default_section_id = resp.getAsJsonObject().get("response").getAsJsonObject().get("catalog").getAsJsonObject().get("default_section").getAsString();

            JsonObject music_section = sections.get(0).getAsJsonObject();
            for( int i = 0 ; i < sections.size() ; i ++){
                if(sections.get(i).getAsJsonObject().get("id").equals(default_section_id)){
                    music_section = sections.get(i).getAsJsonObject();
                    break;
                }
            }

            String next_start = music_section.get("next_from").getAsString();

//            try{
                while (next_start != null){
                    HashMap params = new HashMap<String, String>();
                    params.put("start_from", next_start);
                    params.put("section_id", music_section.get("id").getAsString());
                    JsonElement resp1 = VkUtils.request("catalog.getSection", token, params);
                    if(resp1 == null)
                        for(int k = 0 ; k < 10 ; k++){
                            resp1 = VkUtils.request("catalog.getSection", token, params);
                            if(resp1!=null)
                                break;
                        }

                    JsonElement response = resp1.getAsJsonObject().get("response");
                        if(response != null){
                            JsonElement section = response.getAsJsonObject().get("section");
                            if(section != null){
                                JsonElement next_from = section.getAsJsonObject().get("next_from");
                                if(next_from != null){
                                    next_start = resp1.getAsJsonObject().get("response").getAsJsonObject().get("section").getAsJsonObject().get("next_from").getAsString();
                                }else
                                    break;
                            }else
                                break;
                        }else
                            break;

                    audios.addAll(resp1.getAsJsonObject().get("response").getAsJsonObject().get("audios").getAsJsonArray());

                }

//            }catch(Exception e){
//                Log.e("err", e.toString());
//            }


            isStoragePermissionGranted();

            for(int i = 0 ; i < audios.size() ; i ++){
                Track track = new Track(audios.get(i).getAsJsonObject());
                File folder = new File(Environment.getExternalStorageDirectory().toString() + "/VMUSIC/" + track.getOwnerId());

                if(folder.exists()){
                    File[] files =  folder.listFiles();
                    String fileName = String.format("%s-%s", track.getTitle(), track.getArtist()).replaceAll("\"", "").replaceAll(":", "");

                    for(File f : files){
                        if(f.getName().contains(fileName)){
                            track.progress = 100;
                            track.isLoaded = true;
                            track.setSavedPath(f.toString());
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