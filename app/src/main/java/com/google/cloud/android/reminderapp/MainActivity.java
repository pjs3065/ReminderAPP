/*
 * Copyright 2016 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.android.reminderapp;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageSwitcher;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import com.tsengvn.typekit.TypekitContextWrapper;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;


public class MainActivity extends AppCompatActivity implements MessageDialogFragment.Listener {

    private static final String FRAGMENT_MESSAGE_DIALOG = "message_dialog";

    private static final String STATE_RESULTS = "results";

    Handler mHandler = new Handler();
    static DataBase db;
    private SpeechService mSpeechService;

    private VoiceRecorder mVoiceRecorder;
    VoicePlayer voicePlayer;

    boolean running;
    // View references
    public static TextView mText;
    ImageSwitcher device;
    ImageButton record;
    ImageButton play;
    ImageButton list;

    RecordingSwtich rec = new RecordingSwtich();

    ImageView[] circles = new ImageView[5];
    Handler handler;

    public static Handler vhandler;

    boolean isEnd = false;
    int SampleRate = 16000;
    int BufferSize = 1024;
    private SoundPool sound;
    private int soundbeep;

    TimeAnalysis timeAnalysis;

    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 1; //추가
    boolean isButtonPushed = false; //추가

    public static String fileName;
    String alarmTimeArr[];
    int playCount;

    ListView listView;
    PlaylistAdapter adapter;

    /**
     * @TODO mVoiceCallback 지우기. Main과 Recorder에 있으며 스트리밍을 위한 함수로 보여짐
     * */
    private final VoiceRecorder.Callback mVoiceCallback = new VoiceRecorder.Callback() {
//
//        @Override
//        public void onVoiceStart() {
//            //showStatus(true);
//            if (mSpeechService != null) {
//                mSpeechService.startRecognizing(mVoiceRecorder.getSampleRate());
//            }
//        }
//
//        @Override
//        public void onVoice(byte[] data, int size) {
//            if (mSpeechService != null) {
//                mSpeechService.recognize(data, size);
//            }
//        }
//
//        @Override
//        public void onVoiceEnd() {
//            //showStatus(false);
//            if (mSpeechService != null) {
//                mSpeechService.finishRecognizing();
//            }
//        }
//
    };

    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder binder) {
            mSpeechService = SpeechService.from(binder);
            mSpeechService.addListener(mSpeechServiceListener);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mSpeechService = null;
        }

    };
    int index = 0;

    /**
     * 텍스트, 버튼 등 UI 요소의 클릭 등을 관리하고
     * 핸들러를 통해 Service Search Listener로 부터 받아온 텍스트를 TimeAnalysis로 보내어 분석한다.
     * */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        device = (ImageSwitcher) findViewById(R.id.backgound);
        mText = (TextView) findViewById(R.id.text);
        record = (ImageButton) findViewById(R.id.record);
        play = (ImageButton) findViewById(R.id.play);

        list = (ImageButton) findViewById(R.id.list);

        db = new DataBase(MainActivity.this);
        mVoiceRecorder = new VoiceRecorder(this, mVoiceCallback);
        voicePlayer = new VoicePlayer(this);
        timeAnalysis = new TimeAnalysis();
        device.setEnabled(false);
        mText.setVisibility(View.VISIBLE);

        circles[0] = (ImageView) (findViewById(R.id.circle1));
        circles[1] = (ImageView) (findViewById(R.id.circle2));
        circles[2] = (ImageView) (findViewById(R.id.circle3));
        circles[3] = (ImageView) (findViewById(R.id.circle4));
        circles[4] = (ImageView) (findViewById(R.id.circle5));
        sound = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);
        soundbeep = sound.load(getApplicationContext(), R.raw.rec_start, 1);

        //listing
        listView = (ListView) findViewById(R.id.listView);
        adapter = new PlaylistAdapter();
        alarmTimeArr = db.getAllAlarmTime();
        playCount = alarmTimeArr.length;
        System.out.println("Play Count : " + playCount);
        for(int i = playCount -1; i >= 0; i--) {
            String[] words = alarmTimeArr[i].split(":");
            if(Integer.parseInt(words[3]) < 10) words[3] = '0' + words[3];
            if(Integer.parseInt(words[4]) < 10) words[4] = '0' + words[4];

            String timeRegistered = words[3] + ":" + words[4] + "(" + words[1] + "월" + words[2] + "일" + ")";
            adapter.addItem(new Playlist(timeRegistered));
        }
        // listView.setAdapter(adapter); //추가

        record.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (!mVoiceRecorder.isRecording()) {
                    record.setEnabled(false);
                    record.setVisibility(View.GONE);
                    play.setEnabled(false);
                    play.setVisibility(View.GONE);
                    mText.setText("녹음중 ");
                    mText.setVisibility(View.VISIBLE);
                    device.setEnabled(true);
                    rec.start();
                    sound.play(soundbeep, 1f, 1f, 0, 0, 1);
//                    recordDisplay();
//                    NoticeDisplay();
                    startVoiceRecorder();
                }
            }
        });
        play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (!voicePlayer.isPlaying()) {
                    record.setEnabled(false);
                    record.setVisibility(View.GONE);
                    play.setEnabled(false);
                    play.setVisibility(View.GONE);
                    rec.start();
                    mText.setText("재생중");
                    mText.setVisibility(View.VISIBLE);
                    list.setVisibility(View.VISIBLE);
                    device.setEnabled(true);

//                    playDisplay();
//                    NoticeDisplay();
                    voicePlayer.startPlaying(SampleRate, BufferSize);
                    //TODO 모든 파일의 재생이 완료된 후, 시작 화면으로 전환되도록 개선 필요
                }
            }
        });

        list.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listView.setAdapter(adapter);
                listView.setVisibility(View.VISIBLE);
                mText.setVisibility(View.GONE);
                //list.setVisibility(View.GONE);
            }
        });

        /*
        listView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listView.setVisibility(View.GONE);
                mText.setVisibility(View.VISIBLE);
            }
        });
        */

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                voicePlayer.stopPlaying();
                listView.setVisibility(View.GONE);

                String[] words = alarmTimeArr[(playCount -1) - position].split(":");
                if(Integer.parseInt(words[3]) < 10) words[3] = '0' + words[3];
                if(Integer.parseInt(words[4]) < 10) words[4] = '0' + words[4];
                String timeRegistered = words[3] + ":" + words[4] + "(" + words[1] + "월" + words[2] + "일" + ")";
                System.out.println("재성 " + timeRegistered);
                mText.setText(timeRegistered);
                mText.setVisibility(View.VISIBLE);

                Toast.makeText(getApplicationContext(),(playCount -1) - position + " " + position ,Toast.LENGTH_SHORT).show();
                System.out.println("재성 " + ((playCount -1) - position) + " " + position);

                isEnd = true;


            }
        });

        device.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                System.out.println("MainActivity에서 mVoiceRecorder.isRecording 확인 : " + mVoiceRecorder.isRecording());
                if (mVoiceRecorder.isRecording()) {
                    System.out.println("stop Voice Recorder");
                    sound.play(soundbeep, 1f, 1f, 0, 0, 1);
                    running = false;
                    stopVoiceRecorder();
                }
                if (voicePlayer.isPlaying()) {
                    voicePlayer.stopPlaying();
                    record.setEnabled(true);
                    record.setVisibility(View.VISIBLE);
                    play.setEnabled(true);
                    play.setVisibility(View.VISIBLE);
                    running = false;
                    mText.setText("");
                    mText.setVisibility(View.GONE);
                    device.setEnabled(false);
                }

                if (isEnd) {
                    device.setEnabled(false);
                    record.setEnabled(true);
                    record.setVisibility(View.VISIBLE);
                    play.setEnabled(true);
                    play.setVisibility(View.VISIBLE);
                    mText.setText("");
                    isEnd = false;
                }
            }
        });
        //TODO 음성 정보가 없는 경우(아무 말도 하지 않을 때, 녹음이 되지 않는 경우 등)와 시간 인식이 불가능한 음성정보에 대해서 따로 처리하는 코드가 없고, 현재 시간으로 처리됨. 각각에 대해 따로 처리하여 사용자에게 알려주도로 개선 필요
        handler = new Handler() {
            public void handleMessage(Message msg) {
                String returnedValue = (String) msg.obj;
                String extractValue = new String();

                //아무말 없이 취소했을 경우
                if(returnedValue.equals(""))
                {
                    //mText.setText("터치해주세요");
                    Toast.makeText(getApplicationContext(),"아무말도 안하셨습니다", Toast.LENGTH_LONG).show();
                    isEnd = true;
                    device.callOnClick();
                }
                //TODO 말이 있을 경우 (원하는 답을 찾지 못할때 인식 불가 기능을 추가할 예정)
                else {
                    //TODO 계산된 예정 시간 정보도 데이터베이스에 함께 저장되도록 개선 필요
                    String alarmTime = timeAnalysis.Analysis(returnedValue);
                    String[] words = alarmTime.split(":");
                    if(Integer.parseInt(words[3]) < 10) words[3] = '0' + words[3];
                    if(Integer.parseInt(words[4]) < 10) words[4] = '0' + words[4];

                    String timeRegistered = words[3] + ":" + words[4] + "(" + words[1] + "월" + words[2] + "일" + ")";
                    mText.setText(timeRegistered);

                    db.insert(fileName, alarmTime);

                    Toast.makeText(getApplicationContext(), returnedValue, Toast.LENGTH_LONG).show();
                    isEnd = true;
                }
            }
        };

        vhandler = new Handler() {
            public void handleMessage(Message msg) {
                if(voicePlayer.isPlaying()) {
                    String alarmTime = (String) msg.obj;
                    String[] words = alarmTime.split(":");

                    if (Integer.parseInt(words[3]) < 10) words[3] = '0' + words[3];
                    if (Integer.parseInt(words[4]) < 10) words[4] = '0' + words[4];

                    String timeRegistered = words[3] + ":" + words[4] + "(" + words[1] + "월" + words[2] + "일" + ")";
                    System.out.println("재성(vhandler) " + timeRegistered);
                    mText.setText(timeRegistered);
                }
            }
        };

        device.setFactory(new ViewSwitcher.ViewFactory()
        {
            public View makeView() {
                ImageView imageView = new ImageView(getApplicationContext());
                imageView.setBackgroundColor(0x00000000);
                imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                imageView.setLayoutParams(new ImageSwitcher.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                return imageView;
            }
        });

//        runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                while(true) {
//                    if(mVoiceRecorder.isRecording() || voicePlayer.isPlaying()){
//                        try {
//                            Thread.sleep(100);
//                        } catch (InterruptedException e) {
//                        }
//                        circles[index].setVisibility(View.VISIBLE);
//                        index++;
//                        if (index == 5) {
//                            for (int i = 0; i < 5; i++) {
//                                circles[i].setVisibility(View.INVISIBLE);
//                            }
//                            index = 0;
//                        }
//                    }
//
//
//
//                }
//            }
//        });
    }

    /**
     * MainActivity가 시작할 때, 녹음에 대한 Permission이 있는지 확인하고
     * Permission이 제대로 되어있지 않을 경우 다시 Permission을 받는다.
     */
    @Override
    protected void onStart() {
        super.onStart();

        // Prepare Cloud Speech API
        bindService(new Intent(this, SpeechService.class), mServiceConnection, BIND_AUTO_CREATE);
        // Start listening to voices
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED) {
        } else if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.RECORD_AUDIO)) {
            showPermissionMessageDialog();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO},
                    REQUEST_RECORD_AUDIO_PERMISSION);
        }
    }

    /**
     * MainActivity가 멈출 때, SpeechService를 종료한다.
     */
    @Override
    protected void onStop() {
        // Stop Cloud Speech API
        mSpeechService.removeListener(mSpeechServiceListener);
        unbindService(mServiceConnection);
        mSpeechService = null;

        super.onStop();
    }


    /**
     * Permission을 체크한다. Permission 되어있지 않을 경우 다이얼로그를 통해
     * 유저에게 이를 알린다.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if (permissions.length == 1 && grantResults.length == 1
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startVoiceRecorder();
            } else {
                showPermissionMessageDialog();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    /**
     * 보이스 레코더를 호출하기 위해 사용되는 메소드이다.
     */
    private void startVoiceRecorder() {
//        if (mVoiceRecorder != null) {
//            mVoiceRecorder.stopRecording();
//        }
        mVoiceRecorder.startRecording();
    }

    /**
     * 음성 녹음을 멈춘뒤, 스피치 서비스를 통해 구글 STT 서버로 파일을 전송시킨다.
     */
    private void stopVoiceRecorder() {
        if (mVoiceRecorder != null) {
            System.out.println("녹음을 중지하자.");
            mVoiceRecorder.stopRecording();
            FileInputStream fis = null;
            try {
            //    String fileName = db.getLastFileName(); //전역변수 fileNme에 현재 녹음한 파일이름이 저장돼있음.
                fis = openFileInput(fileName);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            System.out.println("녹음 완료 후, 구글 STT서버로 보내기");
            System.out.println("테스트 파일이름 : " + fileName);
            mSpeechService.recognizeInputStream(fis);
            System.out.println("구글 STT서버로 잘 보내진건가...?");
        }
    }

    /**
     * 녹음과 관련된 Permission을 유저에게 확인받기 위해 다이얼로그를 띄운다.
     */
    private void showPermissionMessageDialog() {
        MessageDialogFragment
                .newInstance(getString(R.string.permission_message))
                .show(getSupportFragmentManager(), FRAGMENT_MESSAGE_DIALOG);
    }


    /**
     * 다이얼로그가 무시되었을 경우 다시 다이얼로그를 띄운다.
     */
    @Override
    public void onMessageDialogDismissed() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO},
                REQUEST_RECORD_AUDIO_PERMISSION);
    }


    /**
     * 구글 STT 서버로 부터 분석된 텍스트를 받아 onCreate에 있는 handler로 전송시킨다.
     *
     * @param text 파일의 음성을 텍스트로 변경한 값
     * @param isFinal 구글 STT 서버로부터 텍스트를 받았는지 아닌지 확인하는 값
     */
    private final SpeechService.Listener mSpeechServiceListener =
            new SpeechService.Listener() {
                @Override
                public void onSpeechRecognized(final String text, final boolean isFinal) {

                    System.out.println("과연4 : " + mText);

                    if (mText != null) {
                        //if (isFinal) {
                        Message message = handler.obtainMessage(1, text);
                        handler.sendMessage(message);
                        //}
                    }
                }
            };

    /**
     * DBInstance를 다른 Class에서도 사용할 수 있도록 하기 위해 사용한다.
     * */
    public static DataBase getDBInstance() {
        return db;
    }


    /**
     * 액티비티의 글꼴을 바꾸기 위해 불러지는 함수이다.
     * CustomStartApp과 연결되어 있다.
     */

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(TypekitContextWrapper.wrap(newBase));
    }

    //    boolean RecRunning = true;
//    void recordDisplay(){
//        runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                while (RecRunning) {
//                    while (mVoiceRecorder.isRecording() == true) {
//                        TimerTask task = new TimerTask() {
//                            @Override
//                            public void run() {
//                                mText.setText("중지시 화면 클릭");
//                            }
//                        };
//                        Timer mTimer = new Timer();
//                        mTimer.schedule(task, 1000);
//                        mText.setText("녹음중");
//                        if (mVoiceRecorder.isRecording() == false)
//                            RecRunning = false;
//                    }
//                }
//            }
//        });
//
//        }
//
//    boolean playRunning =true;
//    void playDisplay() {
//
//        runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                while(playRunning) {
//                    while (voicePlayer.isPlaying() == true) {
//                        TimerTask task = new TimerTask() {
//                            @Override
//                            public void run() {
//                                mText.setText("중지시 화면 클릭");
//                            }
//                        };
//                        Timer mTimer = new Timer();
//                        mTimer.schedule(task, 1000);
//                        mText.setText("재생중");
//                        if(!voicePlayer.isPlaying())
//                            playRunning = false;
//                    }
//                }
//            }
//        });
//
//    }
//
//    boolean noticeRunning = true;
//    void NoticeDisplay() {
//        int duration = 100;
//        runOnUiThread(new Runnable() {
//
//            @Override
//            public void run() {
//                while (noticeRunning) {
//                    while (mVoiceRecorder.isRecording() || voicePlayer.isPlaying()) {
//                        try {
//                            Thread.sleep(100);
//                        } catch (InterruptedException e) {
//                        }
//                        circles[index].setVisibility(View.VISIBLE);
//                        index++;
//                        if (index == 5) {
//                            for (int i = 0; i < 5; i++) {
//                                circles[i].setVisibility(View.INVISIBLE);
//                            }
//                            index = 0;
//                        }
//                        if (!(mVoiceRecorder.isRecording() || voicePlayer.isPlaying()))
//                            noticeRunning = false;
//                    }
//                }
//            }
//        });
//    }
    class RecordingSwtich extends Thread {

        int m_duration;
        final int image_m_Id[] = {R.drawable.display_off,R.drawable.display_off1,R.drawable.display_off2,R.drawable.display_off3,R.drawable.display_off4,R.drawable.display_off5,R.drawable.display_off};
        int m_currentIndex = 0;

        @Override
        public void run() {
            running = true;
            while (running) {
                synchronized (this) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            device.setImageResource(image_m_Id[m_currentIndex]);
                        }
                    });
                    m_currentIndex++;
                    if (m_currentIndex == image_m_Id.length) {
                        m_currentIndex = 0;
                    }
                    try {
                        m_duration = 600;
                        Thread.sleep(m_duration);
                    } catch (InterruptedException e) {
                    }
                }
            }   //while end

        }
    }

    class PlaylistAdapter extends BaseAdapter {
        ArrayList<Playlist> items = new ArrayList<Playlist>();

        @Override
        public int getCount() { return items.size(); }

        public void addItem(Playlist item) { items.add(item); }

        @Override
        public Object getItem(int position) { return items.get(position); }

        @Override
        public long getItemId(int position) { return position; }

        @Override
        public View getView(int position, View convertView, ViewGroup viewGroup) {
            PlaylistView view = new PlaylistView(getApplicationContext());

            Playlist item = items.get(position);
            view.setName(item.getName());
//            view.setMobile(item.getMobile());
//            view.setAge(item.getAge());
//            view.setImage(item.getResId());

            return view;
        }
    }
}