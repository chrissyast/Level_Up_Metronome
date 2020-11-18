package com.example.levelupmetronome;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;


public class MainActivity extends AppCompatActivity {

    private AudioTrack player;

    public void playSound()  {
        player.play();
    }

    public void initialiseSound(int bpm) {
        InputStream firstBeat = getResources().openRawResource(R.raw.beat1);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] b = new byte[1024];
        try {
            for (int readNum; (readNum = firstBeat.read(b)) != -1; ) {
                bos.write(b, 0, readNum);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        int startOfData = bos.toString().indexOf("data")+8;
        byte[] byteArray = bos.toByteArray();
        int millis = 1000 * 60 / bpm;

        try {
            WaveFileReader reader = new WaveFileReader(byteArray);
            byte[] fullLengthByteArray = addSilence(bos, millis, reader);
            int fullBufferSize = fullLengthByteArray.length - startOfData;
            int bytesPerSample = reader.getBitsPerSample() * reader.getNumChannels() / 8;

        player = new AudioTrack.Builder()
                .setTransferMode(AudioTrack.MODE_STATIC)
                .setAudioAttributes(new AudioAttributes.Builder()
                        //        .setUsage(AudioAttributes.USAGE_ALARM)              //   \ not causing problems
                        //        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC) //   / but don't seem necessary
                        //        .setLegacyStreamType(AudioManager.STREAM_MUSIC)  // causes blip on first rep
                        .build())
                .setAudioFormat(new AudioFormat.Builder()
                                 .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                                 .setSampleRate(44100)
                                 .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                        .build())
                .setBufferSizeInBytes(fullBufferSize)
                .setSessionId(AudioManager.AUDIO_SESSION_ID_GENERATE)
                .build();

        player.write(fullLengthByteArray,startOfData,fullLengthByteArray.length - startOfData);
        player.setLoopPoints(0,fullBufferSize/bytesPerSample,-1);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void handleButtonPress(View view) {
        playSound();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initialiseSound(300);

        setContentView(R.layout.activity_main);
    }

    private byte[] addSilence(ByteArrayOutputStream originalSound, int intendedMillis, WaveFileReader reader) {
        int currentDataSize = reader.getDataSize();
        double bytesPerMilli = reader.dataSizePerMillisecond();
        int sampleBytes = reader.getBitsPerSample() * reader.getNumChannels() / 8;
        int intendedDataSize = (int) Math.round((intendedMillis * bytesPerMilli) / sampleBytes) * sampleBytes ;
        int shortFall = intendedDataSize - currentDataSize;
        byte[] origSoundBA = originalSound.toByteArray();
        int origFileSize = origSoundBA.length;
        byte[] output = new byte[origFileSize + shortFall];
        int index = 0;
        for (byte b : origSoundBA) {
            if (index < output.length) {
             output[index] = b;
             index++;
            }
        }
        while (index < output.length) {
            output[index] = 0;
            index++;
        }
        return output;
    }

}