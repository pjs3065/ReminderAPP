
package com.google.cloud.android.reminderapp;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.support.annotation.NonNull;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;


/**
 * Continuously records audio and notifies the {@link VoiceRecorder.Callback} when voice (or any
 * sound) is heard.
 *
 * <p>The recorded audio format is always {@link AudioFormat#ENCODING_PCM_16BIT} and
 * {@link AudioFormat#CHANNEL_IN_MONO}. This class will automatically pick the right sample rate
 * for the device. Use {@link #getSampleRate()} to get the selected value.</p>
 */
public class VoiceRecorder {


    private final int mBufferSize = 1024;
    private final int mBytesPerElement = 2;

    // 설정할 수 있는 sampleRate, AudioFormat, channelConfig 값들을 정의
    // 위의 값들 중 실제 녹음 및 재생 시 선택된 설정값들을 저장
    private int mSampleRate;
    private short mAudioFormat;
    private short mChannelConfig;

    private final Callback mCallback;

    private AudioRecord mRecorder = null;
    private Thread mRecordingThread = null;
    boolean mIsRecording = false;           // 녹음 중인지에 대한 상태값

    DataBase db;

    // 녹음한 파일을 저장할 경로
    // 녹음을 수행할 Thread를 생성하여 녹음을 수행하는 함수
    public void startRecording() {
        mRecorder = null;
        mRecorder = findAudioRecord();
        mRecorder.startRecording();
        mIsRecording = true;
        mRecordingThread = new Thread(new Runnable() {

            @Override
            public void run() {
                writeAudioDataToFile();
            }
        }, "AudioRecorder Thread");
        mRecordingThread.start();
    }
    Context context;
    public VoiceRecorder(Context c, @NonNull Callback callback)
    {
        context =c;
        mCallback = callback;
        db = MainActivity.getDBInstance();
    }

    public static abstract class Callback {

        /**
         * Called when the recorder starts hearing voice.
         */
        public void onVoiceStart() {
        }

        /**
         * Called when the recorder is hearing voice.
         *
         * @param data The audio data in {@link AudioFormat#ENCODING_PCM_16BIT}.
         * @param size The size of the actual data in {@code data}.
         */
        public void onVoice(byte[] data, int size) {
        }

        /**
         * Called when the recorder stops hearing voice.
         */
        public void onVoiceEnd() {
        }
    }

    // 녹음을 하기 위한 sampleRate, audioFormat, channelConfig 값들을 설정
    private AudioRecord findAudioRecord() {
        try {
            int rate = 16000;
            short channel = AudioFormat.CHANNEL_IN_MONO, format = AudioFormat.ENCODING_PCM_16BIT;
            int bufferSize = AudioRecord.getMinBufferSize(rate, channel, format);
            if(bufferSize != AudioRecord.ERROR_BAD_VALUE) {
                mSampleRate = rate;
                mAudioFormat = format;
                mChannelConfig = channel;

                AudioRecord recorder = new AudioRecord(MediaRecorder.AudioSource.DEFAULT, mSampleRate, mChannelConfig, mAudioFormat, bufferSize);

                if (recorder.getState() == AudioRecord.STATE_INITIALIZED) {
                    return recorder;    // 적당한 설정값들로 생성된 Recorder 반환
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;                     // 적당한 설정값들을 찾지 못한 경우 Recorder를 찾지 못하고 null 반환
    }

    // 실제 녹음한 data를 file에 쓰는 함수
    private void writeAudioDataToFile() {

        short sData[] = new short[mBufferSize];
        FileOutputStream fos;
        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yy-MM-dd hh:mm:ss");
        String tempTime = sdf.format(date);
        String fileName = tempTime + ".pcm";
        db.insert(fileName);
        try {
            fos = context.openFileOutput(fileName,context.MODE_PRIVATE);
            while (mIsRecording) {
                int size=mRecorder.read(sData, 0, mBufferSize);
                byte bData[] = short2byte(sData);
                fos.write(bData, 0, mBufferSize * mBytesPerElement);
            }
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // short array형태의 data를 byte array형태로 변환하여 반환하는 함수
    private byte[] short2byte(short[] sData) {
        int shortArrsize = sData.length;
        byte[] bytes = new byte[shortArrsize * 2];
        for (int i = 0; i < shortArrsize; i++) {
            bytes[i * 2] = (byte) (sData[i] & 0x00FF);
            bytes[(i * 2) + 1] = (byte) (sData[i] >> 8);
            sData[i] = 0;
        }
        return bytes;
    }

    // 녹음을 중지하는 함수
    public void stopRecording() {
        if (mRecorder != null) {
            mIsRecording = false;
            mRecorder.stop();
            mCallback.onVoiceEnd();
            mRecorder.release();
        }
    }

    public int getSampleRate() {
        return mRecorder.getSampleRate();
    }
    public int getmBufferSize()
    {
        return mBufferSize;
    }


    public boolean isRecording()
    {
        return mIsRecording;
    }

}