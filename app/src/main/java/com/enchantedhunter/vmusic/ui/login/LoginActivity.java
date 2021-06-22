package com.enchantedhunter.vmusic.ui.login;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.enchantedhunter.vmusic.R;
import com.enchantedhunter.vmusic.common.LocalStorage;
import com.enchantedhunter.vmusic.ui.music.MusicActivity;
import com.enchantedhunter.vmusic.vkutils.VkUtils;

import java.io.IOException;
import java.util.Map;
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

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.getDefaultNightMode());

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.getDefaultNightMode());
        getSupportActionBar().setTitle(getString(R.string.enter));

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
        else if(token.equals(""))
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

        loadingProgressBar.setVisibility(View.VISIBLE);

        Observable.fromCallable(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {

                Map token = null;
                try {
                    token = VkUtils.tryToLogin(usernameEditText.getText().toString(), passwordEditText.getText().toString());
                } catch (IOException e) {
                    e.printStackTrace();
                    return false;
                }

                if(token.get("access_token") != null){
                    try {
                        LocalStorage.setDataInFile(LoginActivity.this, LocalStorage.TOKEN_STORAGE, (String) token.get("access_token"));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }else {
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
            public void onNext(Boolean aBoolean) {
                if(!aBoolean){
                    Toast.makeText(getApplicationContext(), "Ошибка", Toast.LENGTH_SHORT).show();
                    loadingProgressBar.setVisibility(View.GONE);
                }else {
//                            Toast.makeText(getApplicationContext(), "Success", Toast.LENGTH_SHORT).show();
                    startMusicActivity();
                    loadingProgressBar.setVisibility(View.GONE);
                }
            }

            @Override
            public void onError(Throwable e) {
                Toast.makeText(getApplicationContext(), "Ошибка", Toast.LENGTH_SHORT).show();
                loadingProgressBar.setEnabled(false);
            }

            @Override
            public void onComplete() {

            }
        });
    }
}