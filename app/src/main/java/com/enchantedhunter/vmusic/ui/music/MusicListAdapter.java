package com.enchantedhunter.vmusic.ui.music;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.enchantedhunter.vmusic.R;
import com.enchantedhunter.vmusic.data.Track;
import com.enchantedhunter.vmusic.service.AudioService;
import com.enchantedhunter.vmusic.vkutils.VkUtils;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class MusicListAdapter extends RecyclerView.Adapter<MusicListAdapter.TrackViewHolder> {


    public static class TrackViewHolder extends RecyclerView.ViewHolder {

        TextView title;
        TextView artist;
        TextView duration;
        ImageView play;
        ImageView download;
        ProgressBar progressBar;

        TrackViewHolder(View itemView) {
            super(itemView);
            title = (TextView)itemView.findViewById(R.id.title);
            artist = (TextView)itemView.findViewById(R.id.artist);
            duration = (TextView)itemView.findViewById(R.id.duration);
            progressBar = (ProgressBar)itemView.findViewById(R.id.status_progress);

            download = (ImageView)itemView.findViewById(R.id.download_btn_);
            download.setImageResource(R.mipmap.download);

            play = (ImageView)itemView.findViewById(R.id.play_btn_);
            play.setImageResource(R.mipmap.play);

            progressBar.setProgress(0);
        }
    }

    private final List<Track> trackList;
    private final LayoutInflater inflater;
    private final Context context;

    MusicListAdapter(Context context, List<Track> trackList) {
        this.trackList = trackList;
        this.inflater = LayoutInflater.from(context);
        this.context = context;
    }

    public List<Track> getTrackList() {
        return trackList;
    }

    @NonNull
    @Override
    public MusicListAdapter.TrackViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.audio_item, parent, false);
        return new MusicListAdapter.TrackViewHolder(view);
    }

    public  boolean isStoragePermissionGranted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (context.checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                return true;
            } else {
                ActivityCompat.requestPermissions( (Activity) context, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                return false;
            }
        }
        else {
            return true;
        }
    }

    @Override
    public void onBindViewHolder(@NonNull final MusicListAdapter.TrackViewHolder holder, int position) {
        final Track track = trackList.get(position);

        holder.artist.setText(track.getArtist());
        holder.title.setText(position +". "+track.getTitle());
        holder.progressBar.setProgress(track.progress);

        int h = track.getDuration()/3600;
        int m = track.getDuration()/60 - h*60;
        int s = track.getDuration() - h*60*60 - m*60;

        if(track.isLoaded){
            holder.download.setVisibility(View.GONE);
            holder.progressBar.setVisibility(View.GONE);
        }else {
            holder.download.setVisibility(View.VISIBLE);
            holder.progressBar.setVisibility(View.VISIBLE);
        }

        holder.duration.setText( String.format("%02d:%02d:%02d", h, m, s) );

        holder.play.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View view) {
                context.startService(new Intent(context, AudioService.class)
                        .putExtra(AudioService.SERVICE_ACTION, AudioService.ACTION.PLAY.name())
//                        .putExtra(AudioService.AUDIO_TRACKS, (Serializable) trackList)
                        .putExtra(AudioService.AUDIO_TRACK, track));
            }
        });

        holder.setIsRecyclable(false);

        holder.download.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                isStoragePermissionGranted();

                Observable.fromCallable(new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        try {
                            VkUtils.loadTrack(track, context, holder.progressBar);
                        } catch (Exception e) {
                            e.printStackTrace();
                            return false;
                        }
                        return true;
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Boolean>() {

                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(Boolean tracks) {

                        if(tracks){
                            Toast.makeText(context, String.format("%s %s загружен", track.getTitle(), track.getArtist()), Toast.LENGTH_SHORT).show();
                            track.isLoaded = true;
                            holder.download.setVisibility(View.GONE);
                            holder.progressBar.setVisibility(View.GONE);
                        }else {
                            Toast.makeText(context, String.format("%s %s не загружен", track.getTitle(), track.getArtist()), Toast.LENGTH_SHORT).show();
                        }
                        holder.setIsRecyclable(true);

                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onComplete() {

                    }
                });

            }
        });
    }

    @Override
    public int getItemCount() {
        return trackList.size();
    }
}
