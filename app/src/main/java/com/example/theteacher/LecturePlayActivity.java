package com.example.theteacher;

import android.content.Intent;
import android.content.SharedPreferences;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.example.theteacher.NeedToStreaming.encoder.input.video.Camera1ApiManager;
import com.example.theteacher.NeedToStreaming.rtplibrary.rtsp.RtspCamera1;
import com.example.theteacher.NeedToStreaming.rtsp.utils.ConnectCheckerRtsp;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by kimhj on 2018-02-22.
 */

// 이 activity는 강사가 강의를 준비하고 진행하기 위해 사용됩니다.
public class LecturePlayActivity extends AppCompatActivity implements SurfaceHolder.Callback{

    Camera camera;

    RelativeLayout rlLecPlayContainer;
    // 카메라 프리뷰를 띄워줄, 스트리밍 될 영상을 띄워줄 뷰입니다.
    SurfaceView svLecScreen;
    SurfaceHolder shLecScreenHolder;
    // 스트리밍을 시작하거나 정지하기 위해 있는 버튼입니다.
    ImageButton btnLecStart;
    // 학생들간의, 학생들과 강사의 소통을 위해 채팅 메시지를 쯰워주는 listview입니다.
    ListView lvLecChat;
    LecChat_Adapter lecChatAdapter;

    // 채팅 메시지와 관련된 모든 역할을 합니다.
    // 채팅 서버와의 연결부터 메시지 전달, 받기, 종료까지 수행합니다.
    LecChatThread lct;

    // 스트리밍 중이 아니면 1, 스트리밍 중이면 2의 값을 갖습니다.
    // 버튼 하나로 동작을 다르게 하기 위해 쓰는 변수입니다.
    int check;
    String title;
    // 채팅 방 이름을 채팅 서버로 전송하기 위해 사용합니다.
    String rId;

    // rtmp-rtsp-stream-client-java 라이브러리(Apache 2.0) 사용합니다.
    // 소스코드 수정을 위해 필요한 부분만 가져와서 사용했습니다.
    // 다음은 rtsp 시작을 위해 필요한 변수입니다.
    // RtspCamera1로 stream을 시작합니다. 이때 ConnectCheckerRtsp가 필요합니다.
    RtspCamera1 rtspCamera1;
    ConnectCheckerRtsp connectCheckerRtsp;

    // RTSP 서버에 전송할 url입니다.
    // rtsp://ServerIPAddress:port/AppName/UserName 꼴의 url을 사용합니다.
    // sharedpreferences는 user id를 불러와서 UserName 자리에 넣기위해 사용됩니다.
    SharedPreferences sp;
    String url;

    // 서버로 섬네일을 보내는 쓰레드입니다.
    UploadThumbNail uploadThumbNail;
    // 서버로 강의 상태를 전송하는 asynctask입니다.
    // 강의중이면 db에 등록, 아니면 삭제합니다.
    UpdateLecState updateLecState;

    // surfaceView에서 capture된 bitmap이 담길 변수입니다.
    // UploadThumbNail AsyncTask에서 업로드할 때 base64 인코딩되어 전송됩니다.
    public static Bitmap sendBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 상태바 없애고, 화면 켜져있는 상태 유지하도록 설정하기
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.lecture_play);

        check = 1;

        rlLecPlayContainer = (RelativeLayout) findViewById(R.id.rlLecPlayContainer);
        svLecScreen = (SurfaceView) findViewById(R.id.svLecScreen);
        btnLecStart = (ImageButton) findViewById(R.id.btnLecStart);
        lvLecChat = (ListView) findViewById(R.id.lvLecChat);

        btnLecStart.setOnClickListener(btnClickListener);

        shLecScreenHolder = svLecScreen.getHolder();
        shLecScreenHolder.addCallback(this);
        shLecScreenHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        lecChatAdapter = new LecChat_Adapter(getApplicationContext());
        lvLecChat.setAdapter(lecChatAdapter);

        // title은 시작한 강의가 뭔지 서버로 전송할 때 사용합니다.
        // user의 id를 rtsp 경로 구분자로 사용합니다.
        Intent it = getIntent();
        title = it.getStringExtra("title");
        sp = getSharedPreferences("profile", MODE_PRIVATE);
        url = "rtsp://115.71.232.230:81/theteacher/" + sp.getString("id", "")+"_"+title;

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

        // 채팅 서버에 연결합니다.
        rId = sp.getString("id", "")+"_"+title;
        lct = new LecChatThread(getApplicationContext(), lvLecChat, lecChatAdapter, rId);
        lct.start();

    }

    ImageButton.OnClickListener btnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()){
                case R.id.btnLecStart :
                    if(check==1){
                        // 서버로 전송할 데이터를 JSON 형식으로 묶어주는 부분입니다.
                        // 요청한 사람이 누군지 알기 위한 id와 시작한 강의가 뭔지 알려줄 title을 담습니다.
                        // state는 강의 시작인지 정지인지 구분하기 위한 변수입니다.
                        JSONObject jo = new JSONObject();
                        try {
                            jo.put("id", sp.getString("id",""));
                            jo.put("state", "start");
                            jo.put("title", title);
                            Date date = new Date(System.currentTimeMillis());
                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd/HH/mm/ss");
                            jo.put("lasttime", sdf.format(date));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        updateLecState = new UpdateLecState();
                        updateLecState.execute(jo.toString());

                        lct.joinRoom();
                    } else if(check==2){
                        // 서버로 전송할 데이터를 JSON 형식으로 묶어주는 부분입니다.
                        JSONObject jo = new JSONObject();
                        try {
                            jo.put("id", sp.getString("id",""));
                            jo.put("state", "stop");
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        updateLecState = new UpdateLecState();
                        updateLecState.execute(jo.toString());
                        // 채팅방에서 나가도록 서버에 요청합니다.
                        lct.exitRoom();
                    }

                    break;
            }
        }
    };

    @Override
    public void onBackPressed() {
        // 강의를 진행중인 상태에서 실수로 백버튼을 눌러도 종료되지 않도록 처리하였습니다.
        // 만약에 눌르 경우 토스트 메시지를 띄워서 강의를 먼저 종료하도록 유도합니다.
        if(check==1){
            // 강의를 종료하면 썸네일을 더 이상 업로드하지 않도록 업로드 쓰레드도 종료시킵니다.
            if(uploadThumbNail!=null && !uploadThumbNail.isInterrupted()){
                uploadThumbNail.interrupt();
            }
            // 채팅 서버와 소켓 연결이 되어 있는 경우 소켓을 종료시킵니다.
            if(lct.socketChannel.isConnected()){
                try {
                    lct.socketChannel.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            super.onBackPressed();
        } else {
            Toast.makeText(getApplicationContext(), "강의를 먼저 종료해주세요.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(camera!=null){
            camera.stopPreview();
            camera.setPreviewCallback(null);
            camera.release();
            camera = null;
        }
        if(Camera1ApiManager.camera!=null){
            Camera1ApiManager.camera.stopPreview();
            Camera1ApiManager.camera.setPreviewCallback(null);
            Camera1ApiManager.camera.release();
            Camera1ApiManager.camera = null;
        }
        // 채팅 서버와 소켓 연결이 되어잇는 경우 종료시킵니다.
        if(lct.socketChannel.isConnected()){
            try {
                lct.socketChannel.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // SurfaceView에서 캡쳐한 bitmap을 Server(PHP)로 전달하기 위해 Base64 인코딩을합니다.
    // Base64 인코딩은 한글을 리턴합니다.
    public String ConvertBitmapToString(Bitmap bit){
        String encodedImg = "";
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bit.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        encodedImg = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP);
        return encodedImg;
    }

    // 카메라 프리뷰가 실행될 때 자동 실행되는 함수입니다.
    // 여기서 실질적으로 프리뷰되고 있는 화면을 bitmap에 담습니다.
    // 이 액티비티에서 bitmap capture를 하려했었으나 지금은 쓰지 않습니다.
    // Bitmap 추출은 다음 경로의 onPreviewFrame에서 진행됩니다.
    // NeedToStreaming > encoder > input > video > Camera1ApiManager.class
//    Camera.PreviewCallback previewCallback = new Camera.PreviewCallback() {
//        @Override
//        public void onPreviewFrame(byte[] data, Camera camera) {
//            //SurfaceView에 띄워진 Preview를 capture하는 부분입니다.
//            YuvImage img = new YuvImage(data, ImageFormat.NV21, svLecScreen.getWidth(), svLecScreen.getHeight(), null);
//
//            ByteArrayOutputStream baos = new ByteArrayOutputStream();
//            Rect area = new Rect(0, 0, svLecScreen.getWidth(), svLecScreen.getHeight());
//
//            img.compressToJpeg(area, 70, baos);
//            Bitmap tmpBitmap = BitmapFactory.decodeByteArray(baos.toByteArray(), 0, baos.size());
//            sendBitmap = Bitmap.createScaledBitmap(tmpBitmap, 120,140, false);
//        }
//    };

    // 아래 세 개는 SurfaceView를 사용하기 위해 Override되는 부분입니다.
    // 카메라를 불러오고 프리뷰를 띄우거나, 프리뷰를 멈추는 역할을 하게 됩니다.
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        camera = Camera.open();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        try {
            camera.stopPreview();
            camera.setDisplayOrientation(90);

            camera.setPreviewDisplay(shLecScreenHolder);
            camera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if(camera!=null){
            camera.stopPreview();
            camera.setPreviewCallback(null);
            camera.release();
            camera = null;
        }
    }

    // Server의 DB에 Lecture 상태를 업데이트 하기 위한 AsyncTask입니다.
    // 강의중일 경우 DB에 등록하고
    // 강의가 끝날 경우 DB에서 삭제합니다.
    // 강의 시작 : user id, state="start", title엔 강의 제목, 강의를 시작한 시간을 json 형태로 묶어서 전달함
    // 강의 종료 : user id, state="stop"을 json 형태로 묶어서 전달함
    public class UpdateLecState extends AsyncTask<String, String, String>{

        @Override
        protected String doInBackground(String... params) {
            String updateData = params[0];
            Log.i("LecturePlayAct:", "UpdateLecState:"+updateData);
            try{
                URL url = new URL("http://www.o-ddang.com/theteacher/updateLecState.php");
                HttpURLConnection httpURLConnection = (HttpURLConnection)url.openConnection();

                httpURLConnection.setDefaultUseCaches(false);
                httpURLConnection.setDoInput(true);
                httpURLConnection.setDoOutput(true);
                httpURLConnection.setRequestMethod("POST");

                httpURLConnection.setRequestProperty("Accept", "application/json");
                httpURLConnection.setRequestProperty("Content-type", "application/json");

                OutputStream os = httpURLConnection.getOutputStream();
                os.write(updateData.getBytes());
                os.flush();

                InputStreamReader tmp = new InputStreamReader(httpURLConnection.getInputStream(), "EUC-KR");
                BufferedReader reader = new BufferedReader(tmp);
                StringBuilder builder = new StringBuilder();
                String str;
                while ((str = reader.readLine()) != null) {
                    builder.append(str + "\n");
                }
                String result = builder.toString();
                Log.i("LecturePlayAct:", "UpdateLecState:"+result);
                return result;
            } catch (MalformedURLException e) {
                e.printStackTrace();
                return e.getMessage();
            } catch (IOException e) {
                e.printStackTrace();
                return e.getMessage();
            }
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            String result = null;
            try {
                JSONObject jo = new JSONObject(s);
                result = jo.getString("process");
            } catch (JSONException e) {
                e.printStackTrace();
            }

            if(result.equals("updateSuccess")){
                // 강의 시작 요청이 정상적으로 성공했을 때의 응답입니다.
                // 스트리밍을 시작하는 버튼을 클릭했으므로 check 값을 2로 바꿔줍니다.
                check = 2;
                // 스트리밍을 시작하는 부분입니다.
                if (rtspCamera1.prepareAudio() && rtspCamera1.prepareVideo()) {
                    rtspCamera1.startStream(url);
                } else {
                    Toast.makeText(getApplicationContext(), "잠시 후 다시 시도해주세요.", Toast.LENGTH_SHORT).show();
                    finish();
                }
                // 강의를 시작하는 모양의 아이콘을 정지하는 모양의 아이콘으로 바꿔줍니다.
                btnLecStart.setImageResource(R.drawable.rec_stop);
                // 썸네일을 만들어서 서버로 올리는 역할을 하는 쓰레드입니다.
                // 10초에 한 번씩 업로드합니다.
                uploadThumbNail = new UploadThumbNail();
                uploadThumbNail.start();
                Toast.makeText(getApplicationContext(), "강의를 시작합니다.", Toast.LENGTH_SHORT).show();
            } else if(result.equals("updateFail")){
                // 강의 시작 요청이 실패했을 때의 응답입니다.
                Toast.makeText(getApplicationContext(), "잠시 후 다시 시도해주세요.", Toast.LENGTH_SHORT).show();
            } else if(result.equals("deleteSuccess")){
                // 강의 중단 요청이 정상적으로 성공했을 때의 응답입니다.
                // 스트리밍 중에 버튼을 클릭했으므로 정지를 요청하는 것입니다.
                // 스트리밍을 중단한 경우에 check값은 1을 갖습니다.
                check =1;
                rtspCamera1.stopStream();

                // 강의를 정지하는 모양의 아이콘을 시작하는 모양의 아이콘으로 바꿔줍니다.
                btnLecStart.setImageResource(R.drawable.rec_play);
                // 섬네일을 만들어서 서버로 올리는 쓰레드를 정지시키는 부분입니다.
                if(!uploadThumbNail.isInterrupted()){
                    uploadThumbNail.interrupt();
                }
                Toast.makeText(getApplicationContext(), "강의를 종료합니다.", Toast.LENGTH_SHORT).show();
            } else if(result.equals("deleteFail")){
                // 강의 중단 요청이 실패했을 때의 응답입니다.
                Toast.makeText(getApplicationContext(), "다시 요청해주세요.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // SurfaceView에서 추출한 Bitmap을 서버로 전송하는 쓰레드입니다.
    // Bitmap 추출은 다음 경로의 onPreviewFrame에서 진행됩니다.
    // NeedToStreaming > encoder > input > video > Camera1ApiManager.class
    // 전송된 Bitmap은 다른 유저가 볼 수 있는 ThumbNail로 쓰입니다.
    public class UploadThumbNail extends Thread {

        JSONObject jsonObject = new JSONObject();

        public void run() {
            while (true) {
                if (check == 1) {
                    break;
                }
                if(sendBitmap != null){
                    String sessionID = sp.getString("sessionID", "");
                    try {
                        URL url = new URL("http://www.o-ddang.com/theteacher/uploadThumbNail.php");

                        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                        conn.setDoInput(true);
                        conn.setDoOutput(true);
                        conn.setUseCaches(false);
                        conn.setRequestMethod("POST");

                        conn.setInstanceFollowRedirects(false);
                        if (!TextUtils.isEmpty(sessionID)) {
                            conn.setRequestProperty("cookie", sessionID);
                        }

                        conn.setRequestProperty("Accept", "application/json");
                        conn.setRequestProperty("Content-type", "application/json");

                        OutputStream os = conn.getOutputStream();
                        // JSONObject에 capture한 bitmap을 base64 인코딩하여 String으로 변환한 후 담습니다.
                        os.write(jsonObject.put("image", ConvertBitmapToString(sendBitmap)).toString().getBytes());
                        os.flush();

                        int responseStatusCode = conn.getResponseCode();

                        InputStream inputStream;
                        if (responseStatusCode == HttpURLConnection.HTTP_OK) {
                            inputStream = conn.getInputStream();
                        } else {
                            inputStream = conn.getErrorStream();
                        }
                        InputStreamReader isr = new InputStreamReader(inputStream, "UTF-8");
                        BufferedReader rd = new BufferedReader(isr);
                        StringBuilder builder = new StringBuilder();
                        String str = null;
                        while ((str = rd.readLine()) != null) {
                            builder.append(str);
                            Log.i("LecturePlayAct:", "uploadThumbNail:" + str);
                        }
                        String result = builder.toString();

                        os.close();

                        Log.i("LecturePlayAct:", "uploadThumbNail:" + result);

                        // 썸네일 서버로 업로드하는 주기를 설정합니다.
                        // ms 단위로 입력하여 시간을 변경합니다.
                        Thread.sleep(3000);
                    } catch (Exception e) {
                        Log.i("LecturePlayAct:", "uploadThumbNail:" + e.toString());
                    }
                }
            }
        }
    }








}
