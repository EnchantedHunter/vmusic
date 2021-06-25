package com.enchantedhunter.vmusic.ui.music;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.enchantedhunter.vmusic.data.Track;
import com.enchantedhunter.vmusic.service.AudioService;

import java.util.List;

import static com.enchantedhunter.vmusic.service.AudioService.*;
import static com.enchantedhunter.vmusic.service.AudioService.SERVICE_EVENT;

public class MusicReceiver extends BroadcastReceiver {

    private List<Track> tracks = null;

    public MusicReceiver(List<Track> tracks){
        this.tracks = tracks;
    }

    private Track findNext(Track track){

        if(tracks != null){
            int next = -1;
            for(int i = 0 ;i < tracks.size() ; i++){
                if(tracks.get(i).equals(track)){
                    next = i + 1;
                    break;
                }
            }

            if(next >= tracks.size())
                next = 0;

            return tracks.get(next);
        }
        return null;
    }

    private Track findPrev(Track track){

        if(tracks != null){
            int prev = -1;
            for(int i = 0 ;i < tracks.size() ; i++){
                if(tracks.get(i).equals(track)){
                    prev = i - 1;
                    break;
                }
            }

            if(prev < 0)
                prev = tracks.size() - 1;

            return tracks.get(prev);
        }
        return null;
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        if(intent.getStringExtra(SERVICE_EVENT).equals(EVENT.NEXT.name())){

            Track next = findNext ((Track) intent.getExtras().getSerializable(AUDIO_TRACK));

            if(next!=null)
            context.startService(new Intent(context, AudioService.class)
                    .putExtra(SERVICE_ACTION, ACTION.PLAY.name())
                    .putExtra(AudioService.AUDIO_TRACK, next));

        }else if(intent.getStringExtra(SERVICE_EVENT).equals(EVENT.PREV.name())){

            Track prev = findPrev ((Track) intent.getExtras().getSerializable(AUDIO_TRACK));

            if(prev!=null)
                context.startService(new Intent(context, AudioService.class)
                        .putExtra(SERVICE_ACTION, ACTION.PLAY.name())
                        .putExtra(AudioService.AUDIO_TRACK, prev));
        }

    }
}
