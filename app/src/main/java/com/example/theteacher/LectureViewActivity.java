package com.example.theteacher;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.WindowManager;
import android.widget.MediaController;
import android.widget.VideoView;

/**
 * Created by kimhj on 2018-03-03.
 */

public class LectureViewActivity extends AppCompatActivity {

    VideoView vvLecScreen;
    MediaController mediaController;

    String lecUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 상태바 없애고, 화면 켜져있는 상태 유지하도록 설정하기
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.nowview_lecture);

        vvLecScreen = (VideoView)findViewById(R.id.vvLecScreen);

        Intent it = getIntent();
        lecUrl = "rtsp://115.71.232.230:81/theteacher/"+it.getStringExtra("teacherId");

        vvLecScreen.setVideoURI(Uri.parse(lecUrl));
        vvLecScreen.setMediaController(new MediaController(this));
        vvLecScreen.requestFocus();
        vvLecScreen.start();

    }
}
