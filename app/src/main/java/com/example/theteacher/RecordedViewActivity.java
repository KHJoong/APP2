package com.example.theteacher;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.WindowManager;
import android.widget.MediaController;
import android.widget.VideoView;

/**
 * Created by kimhj on 2018-03-24.
 */

public class RecordedViewActivity extends AppCompatActivity {

    // 녹화된 강의를 재생해주는 뷰입니다.
    VideoView vvRecScreen;

    String recUrl;  // 강의 rtsp 스트리밍을 받을 때 사용되는 url입니다.

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 상태바 없애고, 화면 켜져있는 상태 유지하도록 설정하기
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.recview_lecture);

        vvRecScreen = (VideoView) findViewById(R.id.vvRecScreen);

        // MainActivity_NowPlaying에서  Intent로 받은 id를 rtsp url의 구분자로 사용합니다.
        Intent it = getIntent();
        recUrl = "http://115.71.232.230/theteacher/uploaded_video/"+it.getStringExtra("repath");

        vvRecScreen.setVideoURI(Uri.parse(recUrl));
        vvRecScreen.setMediaController(new MediaController(this));
        vvRecScreen.requestFocus();
        vvRecScreen.start();
    }
}
