package com.example.theteacher;

import android.content.SharedPreferences;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageButton;

import com.pedro.rtplibrary.rtsp.RtspCamera1;
import com.pedro.rtsp.utils.ConnectCheckerRtsp;

import java.io.IOException;

/**
 * Created by kimhj on 2018-02-22.
 */

public class LecturePlayActivity extends AppCompatActivity implements SurfaceHolder.Callback{

    Camera camera;

    // 카메라 프리뷰를 띄워줄, 스트리밍 될 영상을 띄워줄 뷰입니다.
    SurfaceView svLecScreen;
    SurfaceHolder svLecScreenHolder;
    // 스트리밍을 시작하거나 정지하기 위해 있는 버튼입니다.
    ImageButton btnLecStart;

    // 스트리밍 중이 아니면 1, 스트리밍 중이면 2의 값을 갖습니다.
    // 버튼 하나로 동작을 다르게 하기 위해 쓰는 변수입니다.
    int check;

    RtspCamera1 rtspCamera1;
    ConnectCheckerRtsp connectCheckerRtsp;

    // RTSP 서버에 전송할 url입니다.
    // rtsp://ServerIPAddress:port/AppName/UserName 꼴의 url을 사용합니다.
    // sharedpreferences는 user id를 불러와서 UserName 자리에 넣기위해 사용됩니다.
    SharedPreferences sp;
    String url;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.lecture_play);

        check = 1;

        svLecScreen = (SurfaceView) findViewById(R.id.svLecScreen);
        btnLecStart = (ImageButton) findViewById(R.id.btnLecStart);

        btnLecStart.setOnClickListener(btnClickListener);

        svLecScreenHolder = svLecScreen.getHolder();
        svLecScreenHolder.addCallback(this);
        svLecScreenHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        sp = getSharedPreferences("profile", MODE_PRIVATE);
        url = "rtsp://115.71.232.230:81/theteacher/" + sp.getString("id", "");

        connectCheckerRtsp = new ConnectCheckerRtsp() {
            @Override
            public void onConnectionSuccessRtsp() {

            }

            @Override
            public void onConnectionFailedRtsp(String s) {

            }

            @Override
            public void onDisconnectRtsp() {

            }

            @Override
            public void onAuthErrorRtsp() {

            }

            @Override
            public void onAuthSuccessRtsp() {

            }
        };

        rtspCamera1 = new RtspCamera1(svLecScreen, connectCheckerRtsp);
    }

    ImageButton.OnClickListener btnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()){
                case R.id.btnLecStart :
                    if(check==1){
                        // 스트리밍을 시작하는 버튼을 클릭했으므로 check 값을 2로 바꿔줍니다.
                        check = 2;
                        // 스트리밍을 시작하는 부분입니다.
                        if (rtspCamera1.prepareAudio() && rtspCamera1.prepareVideo()) {
                            rtspCamera1.startStream(url);
                        } else {
                            /**This device cant init encoders, this could be for 2 reasons: The encoder selected doesnt support any configuration setted or your device hasnt a H264 or AAC encoder (in this case you can see log error valid encoder not found)*/
                        }
                        // 시작시의 아이콘을 정지하는 모양의 아이콘으로 바꿔줍니다.
                        btnLecStart.setImageResource(R.drawable.rec_stop);
                    } else if(check==2){
                        // 스트리밍 중에 버튼을 클릭했으므로 정지를 요청하는 것입니다.
                        // 스트리밍을 중단한 경우에 check값은 1을 갖습니다.
                        check =1;
                        rtspCamera1.stopStream();
                        // 정지시의 아이콘을 시작하기 위한 모양의 아이콘으로 바꿔줍니다.
                        btnLecStart.setImageResource(R.drawable.rec_play);
                    }

                    break;
            }
        }
    };

    // 아래 세 개는 SurfaceView를 사용하기 위해 Override되는 부분입니다.
    // 카메라를 불러오고 프리뷰를 띄우거나, 프리뷰를 멈추는 역할을 하게 됩니다.
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        camera = Camera.open();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        try {
            camera.setPreviewDisplay(svLecScreenHolder);
            camera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        camera.stopPreview();
        camera.release();
        camera = null;
    }
}
