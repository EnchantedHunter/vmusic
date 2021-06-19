package com.enchantedhunter.vmusic.ui.login;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.enchantedhunter.vmusic.R;
import com.enchantedhunter.vmusic.common.LocalStorage;
import com.enchantedhunter.vmusic.ui.music.MusicActivity;
import com.enchantedhunter.vmusic.vkutils.VkUtils;

import java.io.IOException;
import java.util.concurrent.Callable;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class LoginActivity extends AppCompatActivity {

    EditText usernameEditText;
    EditText passwordEditText;
    ProgressBar loadingProgressBar;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        usernameEditText = findViewById(R.id.username);
        passwordEditText = findViewById(R.id.password);
        loadingProgressBar = findViewById(R.id.loading);

        String token = null;
        try {
            token = LocalStorage.getDataFromFile(LoginActivity.this, LocalStorage.TOKEN_STORAGE);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if(token == null)
            return;

        startMusicActivity();

    }

    private void startMusicActivity(){
        // check token
        boolean isActual = true;

        if(isActual){
            this.finish();
            startActivity(new Intent(this, MusicActivity.class));
        }
    }

    public void tryToLogin(View view) {

        loadingProgressBar.setEnabled(true);

        Observable.fromCallable(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {

                String token = null;
                try {
                    token = VkUtils.tryToLogin(usernameEditText.getText().toString(), passwordEditText.getText().toString());
                } catch (IOException e) {
                    return false;
                }

                if(token != null){
                    try {
                        LocalStorage.setDataInFile(LoginActivity.this, LocalStorage.TOKEN_STORAGE, token);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }else {
                    return false;
                }

                // RxJava does not accept null return value. Null will be treated as a failure.
                // So just make it return true.
                return true;
            }
        }) // Execute in IO thread, i.e. background thread.
                .subscribeOn(Schedulers.io())
                // report or post the result to main thread.
                .observeOn(AndroidSchedulers.mainThread())
                // execute this RxJava
                .subscribe(new Observer<Boolean>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(Boolean aBoolean) {
                        if(!aBoolean){
                            Toast.makeText(getApplicationContext(), "Error", Toast.LENGTH_SHORT).show();
                            loadingProgressBar.setEnabled(false);
                        }else {
                            Toast.makeText(getApplicationContext(), "Success", Toast.LENGTH_SHORT).show();
                            startMusicActivity();
                            loadingProgressBar.setEnabled(false);
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        Toast.makeText(getApplicationContext(), "Error", Toast.LENGTH_SHORT).show();
                        loadingProgressBar.setEnabled(false);
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }
}