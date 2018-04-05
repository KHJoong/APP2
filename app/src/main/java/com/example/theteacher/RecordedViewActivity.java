package com.example.theteacher;

import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.VideoView;

/**
 * Created by kimhj on 2018-03-24.
 */

public class RecordedViewActivity extends AppCompatActivity {

    private static final int FLOATING_PERMISSION_CODE = 1;

    // 녹화된 강의를 재생해주는 뷰입니다.
    VideoView vvRecScreen;
    //
    Button btnGoMiniLecture;

    String recUrl;  // 강의 rtsp 스트리밍을 받을 때 사용되는 url입니다.

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 상태바 없애고, 화면 켜져있는 상태 유지하도록 설정하기
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.recview_lecture);

        vvRecScreen = (VideoView) findViewById(R.id.vvRecScreen);
        btnGoMiniLecture = (Button) findViewById(R.id.btnGoMiniLecture);

        // MainActivity_NowPlaying에서  Intent로 받은 id를 rtsp url의 구분자로 사용합니다.
        Intent it = getIntent();
        recUrl = "http://115.71.232.230/theteacher/uploaded_video/"+it.getStringExtra("repath");

        btnGoMiniLecture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkPermission();
            }
        });

        vvRecScreen.setVideoURI(Uri.parse(recUrl));
        vvRecScreen.setMediaController(new MediaController(this));
        vvRecScreen.requestFocus();
        vvRecScreen.start();
    }

    public void checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {   // 마시멜로우 이상일 경우
            if (!Settings.canDrawOverlays(this)) {              // 체크
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, FLOATING_PERMISSION_CODE);
            } else {
                Intent it = new Intent(RecordedViewActivity.this, MiniLectureService.class);
                it.putExtra("lecUrl", recUrl);
                startService(it);
                finish();
            }
        } else {
            Intent it = new Intent(RecordedViewActivity.this, MiniLectureService.class);
            it.putExtra("lecUrl", recUrl);
            startService(it);
            finish();
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FLOATING_PERMISSION_CODE) {
            if (!Settings.canDrawOverlays(this)) {
                // TODO 동의를 얻지 못했을 경우의 처리

            } else {
                Intent it = new Intent(RecordedViewActivity.this, MiniLectureService.class);
                it.putExtra("lecUrl", recUrl);
                startService(it);
                finish();
            }
        }
    }
}
