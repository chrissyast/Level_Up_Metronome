package com.example.levelupmetronome;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Timer;


public class MainActivity extends AppCompatActivity {

    private Timer myTimer;
    private AudioTrack player;

    public void playSound()  {
        player.play();
        player.pause();
        player.reloadStaticData();
        // player.play();
    }

    public void handleButtonPress(View view) {
        //MediaPlayer beat = MediaPlayer.create(this, R.raw.beat1);
        final Handler handler = new Handler(Looper.getMainLooper());
        final Runnable loopSound = new Runnable() {
            @Override
            public void run() {
                playSound();
                handler.postDelayed(this, 300);
            }
        };
        handler.post(loopSound);

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        InputStream foo = getResources().openRawResource(R.raw.beat116);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] b = new byte[1024];
        try {
            for (int readNum; (readNum = foo.read(b)) != -1; ) {
                bos.write(b, 0, readNum);
            }
        } catch (Exception e) {
            System.out.println(e.getStackTrace().toString());
        }
        int startOfData = bos.toString().indexOf("data")+8;

        int minBuffSize = AudioTrack.getMinBufferSize(48000
                , AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT);


        player = new AudioTrack.Builder()
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setLegacyStreamType(AudioManager.STREAM_MUSIC)
                        .build())
                .setAudioFormat(new AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(44100)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                        .build())
                .setBufferSizeInBytes(minBuffSize)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build();

        player.write(bos.toByteArray(),startOfData,bos.toByteArray().length - startOfData);

        setContentView(R.layout.activity_main);
    }
}