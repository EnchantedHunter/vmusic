package com.enchantedhunter.vmusic.data;

import com.google.gson.JsonObject;

public class Track {

    private String artist;
    private String title;
    private String url;
    private String id;
    private String track_id;

    private int duration;

    public int progress = 0;
    public boolean isLoaded = false;
    public String savedPath;

    public Track(JsonObject track) {
//        this.url_im = track.get("image_url").getAsString();
        this.artist = track.get("artist").getAsString();
        this.title = track.get("title").getAsString();
        this.duration = Integer.valueOf(track.get("duration").getAsString());
        this.url = track.get("url").getAsString();
        this.id = track.get("owner_id").getAsString();
        this.track_id = track.get("id").getAsString();
    }

    public Track(){

    }

    @Override
    public boolean equals(Object o) {
        Track track = (Track) o;
        return duration == track.duration &&
                artist.equals(track.artist) &&
                title.equals(track.title) &&
                url.equals(track.url);
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public boolean isSaved(){
        return  isLoaded;
    }

    public String getSavedPath(){
        return savedPath;
    }

    public String getTrackId() {
        return track_id;
    }

    public void setTrackId(String track_id) {
        this.track_id = track_id;
    }
}
