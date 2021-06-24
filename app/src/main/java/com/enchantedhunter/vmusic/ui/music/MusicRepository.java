package com.enchantedhunter.vmusic.ui.music;

import android.content.Context;
import android.os.Environment;

import com.enchantedhunter.vmusic.common.LocalStorage;
import com.enchantedhunter.vmusic.data.Track;
import com.enchantedhunter.vmusic.vkutils.VkUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MusicRepository {

    private JsonElement resp;
    private JsonArray sections;
    private String token = null;
    private String next_start = null;

    public List<Track> refreshMusicList(Context context) throws Exception {

        List<Track> tracks = new ArrayList<>();

        token = LocalStorage.getDataFromFile(context, LocalStorage.TOKEN_STORAGE);
        resp = VkUtils.request("catalog.getAudio", token, new HashMap<String, String>(){{put("need_blocks", "1");}});

        JsonArray audios = resp.getAsJsonObject().get("response").getAsJsonObject().get("audios").getAsJsonArray();

        sections = resp.getAsJsonObject().get("response").getAsJsonObject().get("catalog").getAsJsonObject().get("sections").getAsJsonArray();
        String default_section_id = resp.getAsJsonObject().get("response").getAsJsonObject().get("catalog").getAsJsonObject().get("default_section").getAsString();

        JsonObject music_section = sections.get(0).getAsJsonObject();
        for( int i = 0 ; i < sections.size() ; i ++){
            if(sections.get(i).getAsJsonObject().get("id").equals(default_section_id)){
                music_section = sections.get(i).getAsJsonObject();
                break;
            }
        }

        if(music_section.get("next_from") != null)
            next_start = music_section.get("next_from").getAsString();

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
    }

    public List<Track> tryLoadNext() throws Exception {

        if(next_start == null)
            return null;

        List<Track> tracks = new ArrayList<>();

        JsonObject music_section = sections.get(0).getAsJsonObject();

        HashMap params = new HashMap<String, String>();
        params.put("start_from", next_start);
        params.put("section_id", music_section.get("id").getAsString());
        JsonElement resp1 = VkUtils.request("catalog.getSection", token, params);
        if (resp1 == null)
            for (int k = 0; k < 10; k++) {
                resp1 = VkUtils.request("catalog.getSection", token, params);
                if (resp1 != null)
                    break;
            }

        JsonElement response = resp1.getAsJsonObject().get("response");

        if (response != null) {
            JsonElement section = response.getAsJsonObject().get("section");
            if (section != null) {
                JsonElement next_from = section.getAsJsonObject().get("next_from");
                if (next_from != null) {
                    next_start = resp1.getAsJsonObject().get("response").getAsJsonObject().get("section").getAsJsonObject().get("next_from").getAsString();
                } else {
                    return null;
                }
            } else {
                return null;
            }
        } else{
            return null;
        }

        JsonArray audios = resp1.getAsJsonObject().get("response").getAsJsonObject().get("audios").getAsJsonArray();

        for (int i = 0; i < audios.size(); i++) {
            Track track = new Track(audios.get(i).getAsJsonObject());
            File folder = new File(Environment.getExternalStorageDirectory().toString() + "/VMUSIC/" + track.getOwnerId());

            if (folder.exists()) {
                File[] files = folder.listFiles();
                String fileName = String.format("%s-%s", track.getTitle(), track.getArtist()).replaceAll("\"", "").replaceAll(":", "");

                for (File f : files) {
                    if (f.getName().contains(fileName)) {
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
    }
}
