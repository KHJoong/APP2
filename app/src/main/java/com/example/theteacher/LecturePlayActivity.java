package com.example.theteacher;

import android.content.Intent;
import android.content.SharedPreferences;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import com.pedro.rtplibrary.rtsp.RtspCamera1;
import com.pedro.rtsp.utils.ConnectCheckerRtsp;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
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
    String title;

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

        Intent it = getIntent();
        title = it.getStringExtra("title");
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
                        // 서버로 전송할 데이터를 JSON 형식으로 묶어주는 부분입니다.
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
            if(uploadThumbNail!=null && !uploadThumbNail.isInterrupted()){
                uploadThumbNail.interrupt();
            }
            rtspCamera1.stopStream();
            super.onBackPressed();
        } else {
            Toast.makeText(getApplicationContext(), "강의를 먼저 종료해주세요.", Toast.LENGTH_SHORT).show();
        }
    }

    // 현재 방송중인 화면(SurfaceView)을 Bitmap으로 바꾸는 함수입니다.
    // 서버로 전송하여 ThumbNail을 등록하는데 사용됩니다.
    public Bitmap surfaceToBitmap(View view){
        // 스크린샷 찍어서 Bitmap을 뽑아내는 부분입니다.
        Bitmap getBitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(getBitmap);

        // SurfaceView는 원래 view 중에서 제일 아래에서 진행됩니다.
        // 따라서 setZOrderOnTop를 이용하여 view 최상단으로 올린 후
        // 그것을 canvas(bitmap)에 저장한 후 다시 view의 원래 자리로 돌려줍니다.
        // 이 부분이 없으면 surfaceview에서 screenshot을 찍을 때 검은 화면만 출력됩니다.
        SurfaceView sv = (SurfaceView) view;
        sv.setZOrderOnTop(true);
        sv.draw(canvas);
        sv.setZOrderOnTop(false);

        // 뽑아낸 Bitmap을 Resize하는 부분입니다.
//        Matrix mat = new Matrix();
//        mat.postScale((float) 0.1, (float) 0.1);
//        Bitmap returnBitmap = Bitmap.createBitmap(getBitmap, 0, 0, view.getWidth(), view.getHeight(), mat, false);
//        getBitmap.recycle();

        // Resize된 Bitmap을 반환합니다.
        return getBitmap;
    }

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
        if(camera!=null){
            camera.stopPreview();
            camera.release();
            camera = null;
        }
    }

    // DB에 Lecture 상태를 업데이트 하기 위한 AsyncTask입니다.
    // 강의중일 경우 DB에 등록하고
    // 강의가 끝날 경우 DB에서 삭제합니다.
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
                Bitmap bt = surfaceToBitmap(svLecScreen);
                uploadThumbNail = new UploadThumbNail(bt);
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
    // 전송된 Bitmap은 다른 유저가 볼 수 있는 ThumbNail로 쓰입니다.
    public class UploadThumbNail extends Thread {

        Bitmap sendedBitmap;

        String lineEnd = "\r\n";
        String twoHypens = "--";
        String boundary = "*****";

        public UploadThumbNail(Bitmap bm) {
            sendedBitmap = bm;
        }

        public void run() {
            while (true) {
                if (check == 1) {
                    break;
                }
                try {
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
                        conn.setRequestProperty("Connection", "Keep-Alive");
                        conn.setRequestProperty("Cache-Control", "no-cache");
                        conn.setRequestProperty("ENCTYPE", "multipart/form-data");
                        conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);

                        DataOutputStream dos = new DataOutputStream(conn.getOutputStream());

                        dos.writeBytes(twoHypens + boundary + lineEnd);
                        dos.writeBytes("Content-Disposition: form-data; name=\"uploaded_image\"" + lineEnd);
                        dos.writeBytes(lineEnd);
                        dos.writeBytes("uploaded_thumbnail");
                        dos.writeBytes(lineEnd);

                        dos.writeBytes(twoHypens + boundary + lineEnd);
                        dos.writeBytes("Content-Disposition: form-data; name=\"uploaded_file\"; filename=\"" + sp.getString("id", "") + ".jpg" + "\"" + lineEnd);
                        dos.writeBytes(lineEnd);

                        byte[] pixels = new byte[sendedBitmap.getWidth() * sendedBitmap.getHeight()];
                        for (int i = 0; i < sendedBitmap.getWidth(); ++i) {
                            for (int j = 0; j < sendedBitmap.getHeight(); ++j) {
                                pixels[i + j] = (byte) ((sendedBitmap.getPixel(i, j) & 0x80) >> 7);
                            }
                        }
                        dos.write(pixels);

                        dos.writeBytes(lineEnd);
                        dos.writeBytes(twoHypens + boundary + twoHypens + lineEnd);

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

                        dos.flush();
                        dos.close();

                        Log.i("LecturePlayAct:", "uploadThumbNail:" + result);
                    } catch (Exception e) {
                        Log.i("LecturePlayAct:", "uploadThumbNail:" + e.toString());
                    }
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }








}
