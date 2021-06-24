package com.enchantedhunter.vmusic.ui.music;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.enchantedhunter.vmusic.data.Track;
import com.enchantedhunter.vmusic.service.AudioService;

import java.util.List;

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

    @Override
    public void onReceive(Context context, Intent intent) {

        switch (intent.getStringExtra(AudioService.SERVICE_EVENT)){
            case AudioService.NOTIFICATION_ACTION_NEXT:

                Track next = findNext ((Track) intent.getExtras().getSerializable(AudioService.AUDIO_TRACK));

                if(next!=null)
                context.startService(new Intent(context, AudioService.class)
                        .putExtra(AudioService.SERVICE_ACTION, AudioService.ACTION.PLAY.name())
                        .putExtra(AudioService.AUDIO_TRACK, next));

                break;
        }

    }
}
