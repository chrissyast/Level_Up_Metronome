package com.example.levelupmetronome;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;


public class MainActivity extends AppCompatActivity {

    private AudioTrack player;
    private int bpm;
    private int beatsPerBar;

    private void playSound()  {
        player.play();
    }

    private int millis() {
        return 1000 * 60 / bpm;
    }

    private void initialiseSound() {
        InputStream firstBeat = getResources().openRawResource(R.raw.beat1);
        InputStream otherBeat = getResources().openRawResource(R.raw.otherbeats);

        try {
            WaveFileReader firstBeatReader = new WaveFileReader(firstBeat);
            WaveFileReader otherBeatReader = new WaveFileReader(otherBeat);
            byte[] fullLengthFirstBeat = createBeatWithSilence(firstBeatReader);
            byte[] fullLengthOtherBeat = createBeatWithSilence(otherBeatReader);
            ByteArrayOutputStream finalSound = new ByteArrayOutputStream();
            for (int i = 1; i <= beatsPerBar; i++) {
                if (i==1) {
                    finalSound.write(fullLengthFirstBeat);
                } else {
                    finalSound.write(fullLengthOtherBeat);
                }
            }

            byte[] finalBytes = finalSound.toByteArray();

            int fullBufferSize = (fullLengthFirstBeat.length) * beatsPerBar;
            int bytesPerSample = firstBeatReader.getBitsPerSample() * firstBeatReader.getNumChannels() / 8;

            instantiatePlayer(firstBeatReader, fullBufferSize);

        player.write(finalBytes,0,finalBytes.length);
        player.setLoopPoints(0,fullBufferSize/bytesPerSample,-1);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void instantiatePlayer(WaveFileReader reader, int fullBufferSize) {
        if (Build.VERSION.SDK_INT >= 23) {
            player = new AudioTrack.Builder()
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .setAudioAttributes(new AudioAttributes.Builder()
                            //        .setUsage(AudioAttributes.USAGE_ALARM)              //   \ not causing problems
                            //        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC) //   / but don't seem necessary
                            //        .setLegacyStreamType(AudioManager.STREAM_MUSIC)  // causes blip on first rep
                            .build())
                    .setAudioFormat(new AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(reader.getSampleRate())
                            .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                            .build())
                    .setBufferSizeInBytes(fullBufferSize)
                    .setSessionId(AudioManager.AUDIO_SESSION_ID_GENERATE)
                    .build();
        } else {
            player = new AudioTrack(AudioManager.STREAM_MUSIC, reader.getSampleRate(), AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT, fullBufferSize, AudioTrack.MODE_STATIC);
        }


    }

    public void handleButtonPress(View view) {
        playSound();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        bpm = 120;
        beatsPerBar = 5;
        initialiseSound();

        setContentView(R.layout.activity_main);
    }

    private byte[] createBeatWithSilence(WaveFileReader reader) {
        int intendedMillis = millis();
        byte[] originalSound = reader.getDataChunk();
        int currentDataSize = reader.getDataSize();
        double bytesPerMilli = reader.getByteRate() / 1000;
        int sampleBytes = reader.getBitsPerSample() * reader.getNumChannels() / 8;
        int intendedDataSize = (int) Math.round((intendedMillis * bytesPerMilli) / sampleBytes) * sampleBytes ;
        int shortFall = intendedDataSize - currentDataSize;
        int origFileSize = originalSound.length;
        byte[] output = new byte[origFileSize + shortFall];
        int index = 0;
        for (byte b : originalSound) {
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