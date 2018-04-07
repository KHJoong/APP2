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
    // 강의를 창모드로 보고 싶은 경우 사용할 버튼
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
                // 창모드로 보길 원하는 경우
                // ACTION_MANAGE_OVERLAY_PERMISSION 에 대한 권한을 얻어야 함
                // 유저가 권한에 동의했으면 창모드로 실행, 안했으면 실행하지 않음
                checkPermission();
            }
        });

        vvRecScreen.setVideoURI(Uri.parse(recUrl));
        vvRecScreen.setMediaController(new MediaController(this));
        vvRecScreen.requestFocus();
        vvRecScreen.start();
    }

    // 창모드로 강의를 띄워주는 함수
    public void GoMiniLecture(){
        Intent it = new Intent(RecordedViewActivity.this, MiniLectureService.class);
        it.putExtra("lecUrl", recUrl);
        it.putExtra("current", vvRecScreen.getCurrentPosition());
        startService(it);
        finish();
    }

    // 유저가 권한을 동의 했는지, 하지 않았는지 체크하는 부분
    public void checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, FLOATING_PERMISSION_CODE);
            } else {
                // 이미 동의 한 유저의 경우 바로 창모드로 띄워주도록 실행
                GoMiniLecture();
            }
        } else {
            // 안드로이드 버전이 마시멜로우 이하일 경우 권한 확인하지 않고 실행
            GoMiniLecture();
        }
    }

    // 유저가 권한이 아직 없는 경우 물어보는 부분
    @TargetApi(Build.VERSION_CODES.M)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FLOATING_PERMISSION_CODE) {
            if (!Settings.canDrawOverlays(this)) {
                // 동의를 하지 않았을 경우 아무 작업도 하지 않음

            } else {
                // 동의할 경우 창모드를 띄워줌
                GoMiniLecture();
            }
        }
    }
}
