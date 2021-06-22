package com.enchantedhunter.vmusic.data;

import com.google.gson.JsonObject;

import java.io.Serializable;

public class Track implements Serializable {

    private String artist;
    private String title;
    private String url;
    private String ownerId;
    private String trackId;
    private String savedPath;

    private int duration;
    public int progress = 0;
    public boolean isLoaded = false;

    public Track(JsonObject track) {
        this.artist = track.get("artist").getAsString();
        this.title = track.get("title").getAsString();
        this.duration = Integer.valueOf(track.get("duration").getAsString());
        this.url = track.get("url").getAsString();
        this.ownerId = track.get("owner_id").getAsString();
        this.trackId = track.get("id").getAsString();
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

    public boolean isSaved(){
        return  isLoaded;
    }

    public String getTrackId() {
        return trackId;
    }

    public void setTrackId(String trackId) {
        this.trackId = trackId;
    }

    public String getOwnerId() { return ownerId; }

    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }

    public void setSavedPath(String savedPath) { this.savedPath = savedPath; }

    public String getSavedPath(){
        return savedPath;
    }
}
