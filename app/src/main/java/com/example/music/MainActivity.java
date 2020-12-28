package com.example.music;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};
    private ListView musicList;
    private int counter = 0;
    private int setter = 0;
    private TextView mName;
    private TextView aName;
    private TextView current;
    private TextView max;
    private SeekBar seek;
    private List<MusicItem> mItems;
    private ArrayList<MediaPlayer> mp = new ArrayList<MediaPlayer>();
    private Runnable func;
    private Handler handler = new Handler();
    private boolean flgPrev = false;

    public MainActivity() {
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        int permission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(this, PERMISSIONS_STORAGE, REQUEST_EXTERNAL_STORAGE);
        }

        mName = findViewById(R.id.textView);
        current = findViewById(R.id.textView2);
        max = findViewById(R.id.textView3);
        aName = findViewById(R.id.textView4);
        seek = findViewById(R.id.seekBar);
        ImageButton play = findViewById(R.id.imageButton3);
        ImageButton prev = findViewById(R.id.imageButton4);
        ImageButton next = findViewById(R.id.imageButton5);
        ImageButton pause = findViewById(R.id.imageButton6);
        ImageButton loopOff = findViewById(R.id.imageButton);
        ImageButton loopOn = findViewById(R.id.imageButton2);
        musicList = (ListView) findViewById(R.id.musicView);

        //端末内の曲リスト
        mItems = MusicItem.getItems(getApplicationContext());
        int size = mItems.size();
        String[] name = new String[size];

        //sample
        String[] sample = {"Couldn't find files"};

        for (int i = 0; i < size; i++) {
            name[i] = mItems.get(i).title;
            mp.add(new MediaPlayer());
        }

        ArrayAdapter adapter;
        if (size == 0) {
            adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, sample);
        } else {
            adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, name);
        }
        musicList.setAdapter(adapter);

        //プレイヤー設定
        mp.get(counter).setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build());
        setMusic(mp.get(counter), counter);
        initMPAll();
        setter = 0;
        mName.setText(mItems.get(counter).title);
        aName.setText(mItems.get(counter).artist);
        setMax();

        //イベントリスナ設定
        //play
        play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //再生処理(ポーズ中なら途中から，非再生時は最初の曲から)
                mp.get(counter).start();

                //ボタン切り替え
                play.setVisibility(View.INVISIBLE);
                pause.setVisibility(View.VISIBLE);
            }
        });

        //pause
        pause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //中断処理(再生中に一時停止　再生時以外は動作しない)
                if (mp.get(counter).isPlaying()) {
                    mp.get(counter).pause();
                }

                //ボタン切り替え
                pause.setVisibility(View.INVISIBLE);
                play.setVisibility(View.VISIBLE);
            }
        });

        //next
        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //スキップ処理(次の曲を再生)
                mp.get(counter).seekTo(mp.get(counter).getDuration());
            }
        });

        //prev
        prev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //バック処理(前の曲を再生(最初の曲の場合は最初にシーク))
                if(counter == 0){
                    mp.get(counter).seekTo(0);
                }else{
                    flgPrev = true;
                    mp.get(counter).seekTo(mp.get(counter).getDuration());
                }
            }
        });

        //loopOn
        loopOn.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                //クリックで色変化 & ループOFF
                if(mp.get(counter).isLooping()){
                    mp.get(counter).setLooping(false);
                    loopOn.setVisibility(View.INVISIBLE);
                    loopOff.setVisibility(View.VISIBLE);
                }
            }
        });

        //loopOff
        loopOff.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                //クリックで色変化 & ループON
                if(!mp.get(counter).isLooping()){
                    mp.get(counter).setLooping(true);
                    loopOff.setVisibility(View.INVISIBLE);
                    loopOn.setVisibility(View.VISIBLE);
                }
            }
        });

        //リストアイテムのリスナ
        musicList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //クリックされた曲を再生
                mp.get(counter).stop();
                mp.get(counter).release();
                initMP();
                counter = position;
                mName.setText(mItems.get(counter).title);
                aName.setText(mItems.get(counter).artist);
                setMax();
                play.setVisibility(View.INVISIBLE);
                pause.setVisibility(View.VISIBLE);
                mp.get(counter).start();
            }
        });

        //シークバー変更リスナ
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(fromUser){
                    mp.get(counter).seekTo(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                //hoge
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                //huga
            }
        });

        for (int i = 0; i < mItems.size(); i++) {
            //エラー状態リスナ
            mp.get(i).setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    mp.reset();
                    return false;
                }
            });

            //再生準備完了
            mp.get(i).setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp1) {
                    play.setVisibility(View.VISIBLE);
                }
            });

            //再生完了時
            mp.get(i).setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp1) {
                    mp.get(counter).release();
                    if (counter + 1 >= mp.size() && !flgPrev) {
                        initMP();
                        counter = 0;
                        //シークバー & 再生時間初期化
                        seek.setProgress(0);
                        current.setText("0:00");
                        mName.setText(mItems.get(counter).title);
                        aName.setText(mItems.get(counter).artist);
                        // ボタン初期化
                        pause.setVisibility(View.INVISIBLE);
                        play.setVisibility(View.VISIBLE);
                    } else {
                        initMP();
                        if(flgPrev){
                            if(counter != 0){
                                counter--;
                                flgPrev = false;
                            }
                        }else if(mp.get(counter).isLooping()){
                            //counter 何もしない
                        }else{
                            counter++;
                        }

                        mName.setText(mItems.get(counter).title);
                        aName.setText(mItems.get(counter).artist);
                        setMax();
                        mp.get(counter).start();
                    }
                }
            });
        }

        //再生時間
        func = () -> {
            String time;
            if(counter >= 0){
                if((mp.get(counter) != null) && mp.get(counter).isPlaying()){
                    int progress = mp.get(counter).getCurrentPosition();
                    int min = (progress/1000)/60;
                    int sec = (progress/1000) % 60;
                    if (sec < 10)
                        time = "0" + sec;
                    else
                        time = "" + sec;
                    String elapsedTime = min + ":" + time + "";
                    current.setText(elapsedTime);
                    seek.setMax(mp.get(counter).getDuration());
                    seek.setProgress(progress);
                }
                handler.postDelayed(func, 1000);
            }
        };
        handler.postDelayed(func, 1000);
    }

    //メソッド
    //曲の長さ
    private void setMax() {
        String maxTime;
        int time = mp.get(counter).getDuration();
        int min = (time/1000)/60;
        Log.i("auto","seek");
        int sec = (time/1000) % 60;
        if (sec < 10)
            maxTime = "0" + sec;
        else
            maxTime = "" + sec;
        String musicTime = min + ":" + maxTime + "";
        max.setText(musicTime);
    }

    //再生準備
    private void setMusic(MediaPlayer mp, int index) {
        if (!mItems.isEmpty()) {
            try {
                mp.setDataSource(getApplicationContext(), mItems.get(index).getURI());
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                mp.prepare();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            mName.setText("Error");
            aName.setText("Couldn't Read File");
        }
    }

    //全体初期化
    private void initMPAll() {
        if (setter + 1 < mItems.size()) {
            mp.get(setter + 1).setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build());
            setMusic(mp.get(setter + 1), setter + 1);
            setter++;
            initMPAll();
        }
    }

    //再生終了後の個別初期化
    private void initMP() {
        mp.remove(counter);
        mp.add(counter, new MediaPlayer());
        mp.get(counter).setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build());
        setMusic(mp.get(counter), counter);
    }
}