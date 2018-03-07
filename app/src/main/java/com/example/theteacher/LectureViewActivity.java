package com.example.theteacher;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.MediaController;
import android.widget.VideoView;

import java.io.IOException;

/**
 * Created by kimhj on 2018-03-03.
 */

// 이 activity는 학생이 강의를 볼 때 열립니다.
public class LectureViewActivity extends AppCompatActivity {

    // 실시간 강의를 재생해주는 뷰입니다.
    VideoView vvLecScreen;
    // 채팅 메시지를 입력하기 위한 곳입니다.
    EditText etLecChat;
    // 채팅 메시지를 전송할 때 사용되는 버튼입니다.
    Button btnLecChatSend;
    // 학생들간의, 학생들과 강사의 소통을 위해 채팅 메시지를 쯰워주는 listview입니다.
    ListView lvLecChat;
    LecChat_Adapter lecChatAdapter;

    // 채팅 메시지와 관련된 모든 역할을 합니다.
    // 채팅 서버와의 연결부터 메시지 전달, 받기, 종료까지 수행합니다.
    LecChatThread lct;

    String lecUrl;  // 강의 rtsp 스트리밍을 받을 때 사용되는 url입니다.
    String lecTitle; // 강의 제목입니다.
    String rId; // 채팅 방에 입장하기 위한 방 구분자(방 이름)입니다.("강사id_강의제목"의 형태로 만들어져 있습니다.)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 상태바 없애고, 화면 켜져있는 상태 유지하도록 설정하기
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.nowview_lecture);

        vvLecScreen = (VideoView)findViewById(R.id.vvLecScreen);
        etLecChat = (EditText) findViewById(R.id.etLecChat);
        btnLecChatSend = (Button) findViewById(R.id.btnLecChatSend);
        lvLecChat = (ListView) findViewById(R.id.lvLecChat);

        lecChatAdapter = new LecChat_Adapter(getApplicationContext());
        lvLecChat.setAdapter(lecChatAdapter);

        btnLecChatSend.setOnClickListener(btnClickListener);

        // MainActivity_NowPlaying에서  Intent로 받은 id를 rtsp url의 구분자로 사용합니다.
        Intent it = getIntent();
        lecUrl = "rtsp://115.71.232.230:81/theteacher/"+it.getStringExtra("teacherId");
        lecTitle = it.getStringExtra("lecTitle");

        // 채팅 서버와 연결하는 부분입니다.
        rId = it.getStringExtra("teacherId")+"_"+lecTitle;
        lct = new LecChatThread(getApplicationContext(), lvLecChat, lecChatAdapter, rId);
        lct.start();

        vvLecScreen.setVideoURI(Uri.parse(lecUrl));
        vvLecScreen.setMediaController(new MediaController(this));
        vvLecScreen.requestFocus();
        vvLecScreen.start();
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true){
                    if(vvLecScreen.isPlaying()){
                        lct.joinRoom();
                        etLecChat.setVisibility(View.VISIBLE);
                        btnLecChatSend.setVisibility(View.VISIBLE);
                        break;
                    }
                }
            }
        }).start();

    }

    Button.OnClickListener btnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()){
                case R.id.btnLecChatSend :
                    lct.sendLecChat(etLecChat.getText().toString());

                    etLecChat.setText("");
                    break;
            }
        }
    };

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        lct.exitRoom();
        if(lct.socketChannel.isConnected()){
            try {
                lct.socketChannel.finishConnect();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
