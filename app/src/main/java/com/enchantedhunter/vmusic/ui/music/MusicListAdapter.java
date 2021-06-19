package com.enchantedhunter.vmusic.ui.music;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.enchantedhunter.vmusic.R;
import com.enchantedhunter.vmusic.data.Track;
import com.enchantedhunter.vmusic.vkutils.VkUtils;

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
        ProgressBar progressBar;

        TrackViewHolder(View itemView) {
            super(itemView);
            title = (TextView)itemView.findViewById(R.id.title);
            artist = (TextView)itemView.findViewById(R.id.artist);
            progressBar = (ProgressBar)itemView.findViewById(R.id.status_progress);
            progressBar.setProgress(0);
//            personPhoto = (ImageView)itemView.findViewById(R.id.person_photo);
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

    @NonNull
    @Override
    public MusicListAdapter.TrackViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.audio_item, parent, false);
        return new MusicListAdapter.TrackViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final MusicListAdapter.TrackViewHolder holder, int position) {
        final Track track = trackList.get(position);

        holder.artist.setText(track.getArtist());
        holder.title.setText(track.getTitle());
        holder.progressBar.setProgress(track.progress);
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

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
                            Toast.makeText(context, "Downloaded", Toast.LENGTH_SHORT).show();
                        }else {
                            Toast.makeText(context, "Download Error", Toast.LENGTH_SHORT).show();
                        }
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
