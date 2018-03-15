package com.example.theteacher;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.channels.SocketChannel;

/**
 * Created by kimhj on 2018-03-13.
 */

// 이 클래스는 다른 사용자와 함께 사진 위에 그린 그림을 공유하기 위해 만든 화면입니다.
// 모든 static 변수는 이 클래스와 DrawingView에서 함께 사용하기 위해 사용되었습니다.
public class QuestionViewActivity extends AppCompatActivity{

    // Custom View입니다. 클릭하여 움직인 부분에 선을 그려줍니다.
    DrawingView dv;
    static Paint mPaint;

    // 연결될 채팅 서버의 ip와 그때 사용되는 port 번호입니다.
    String ip = "115.71.232.230";
    int port = 6666;
    static SocketChannel socketChannel;

    SharedPreferences sp;
    static JSONObject jo;
    static Handler handler;

    AlertDialog.Builder adBuilder;
    EditText etQueTitle;
    // 질문의 제목을 담을 변수입니다.
    String queTitle;
    // userType은 my 또는 other입니다.
    // my는 작성자, other은 질문을 해결해주기 위해 방에 참가한 사람을 의미합니다.
    String userType;
    static String picUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 상태바 없애고, 화면 켜져있는 상태 유지하도록 설정하기
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.question_view);

        Intent it = getIntent();
        picUrl = it.getStringExtra("picUrl");

        dv = (DrawingView)findViewById(R.id.dv);

        sp = getSharedPreferences("profile", MODE_PRIVATE);
        jo = new JSONObject();
        handler = new Handler();

        userType = it.getStringExtra("type");
        if(userType.equals("my")){
            // 질문의 작성자가 이 화면에 들어왔을 경우입니다.
            // Dialog를 이용하여 질문의 제목을 선택하도록 합니다.
            // 질문 등록을 완료하면 소켓에 연결됩니다.
            adBuilder = new AlertDialog.Builder(this);
            adBuilder.setTitle("질문 제목 입력");
            adBuilder.setMessage("질문을 등록하기 위해 제목을 입력해주세요");
            etQueTitle = new EditText(this);
            adBuilder.setView(etQueTitle);
            adBuilder.setPositiveButton("등록", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    queTitle = etQueTitle.getText().toString();

                    connectSocket cs = new connectSocket();
                    cs.start();

                    updateQuestion uq = new updateQuestion();
                    uq.execute();
                }
            });
            adBuilder.show();
        } else if(userType.equals("other")){
            // 질문에 대답하기 위해 방에 들어온 사람을 위한 경우입니다.
            // 소켓 연결만 진행할 수 있도록 합니다.
            queTitle = it.getStringExtra("title");

            connectSocket cs = new connectSocket();
            cs.start();
        }

        // 뷰를 터치할 때 그려지는 선을 셋팅해주는 부분입니다.
        // 초록색 형광 선을 그리도록 합니다.
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mPaint.setColor(Color.GREEN);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setStrokeWidth(10);
    }

    @Override
    public void onBackPressed() {
        // 뒤로가기 버튼을 클릭했을 때 진짜 종료할 것인지 물어보는 부분입니다.
        adBuilder = new AlertDialog.Builder(this);
        adBuilder.setTitle("종료하시겠습니까?");
        adBuilder.setPositiveButton("확인", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                exitQuestion eq = new exitQuestion();
                eq.start();
            }
        });
        adBuilder.show();

    }

    // 이 thread는 질문자가 질문을 종료하겠다고 할 때 실행합니다.
    // 소켓 연결을 종료하고 서버에 등록했던 질문을 삭제한 후 화면을 끕니다.
    public class exitQuestion extends Thread{

        @Override
        public void run() {
            super.run();
            try {
                jo.put("userId", sp.getString("id", ""));
                jo.put("type", "exit_room");
                jo.put("roomId", queTitle);

                if(socketChannel !=null){
                    socketChannel.socket().getOutputStream().write(jo.toString().getBytes("EUC-KR"));
                }

                if(userType.equals("my")){
                    try{
                        URL url = new URL("http://www.o-ddang.com/theteacher/delQuestion.php");
                        HttpURLConnection httpURLConnection = (HttpURLConnection)url.openConnection();

                        httpURLConnection.setDefaultUseCaches(false);
                        httpURLConnection.setDoInput(true);
                        httpURLConnection.setDoOutput(true);
                        httpURLConnection.setRequestMethod("POST");

                        httpURLConnection.setRequestProperty("Accept", "application/json");
                        httpURLConnection.setRequestProperty("Content-type", "application/json");

                        OutputStream os = httpURLConnection.getOutputStream();
                        os.write(jo.toString().getBytes());
                        os.flush();

                        int responseStatusCode = httpURLConnection.getResponseCode();

                        InputStream inputStream;
                        if(responseStatusCode == HttpURLConnection.HTTP_OK) {
                            inputStream = httpURLConnection.getInputStream();
                        } else{
                            inputStream = httpURLConnection.getErrorStream();
                        }
                        InputStreamReader isr = new InputStreamReader(inputStream, "UTF-8");
                        BufferedReader rd = new BufferedReader(isr);
                        StringBuilder builder = new StringBuilder();
                        String str = null;
                        while((str = rd.readLine()) != null){
                            builder.append(str);
                            Log.i("MainAct_Question:", "UpLoadQuestionPic:"+str);
                        }
                        String result = builder.toString();
                        Log.i("QuestionViewAct:", result);
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                        Log.i("QuestionViewAct:", e.getMessage());
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.i("QuestionViewAct:", e.getMessage());
                    }
                }

                if (socketChannel.isConnected() && socketChannel!=null) {
                    socketChannel.close();
                    dv.receiveStop();
                }
            } catch (JSONException e) {
                e.printStackTrace();
                Log.i("QuestionViewAct:", e.getMessage());
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                Log.i("QuestionViewAct:", e.getMessage());
            } catch (IOException e) {
                e.printStackTrace();
                Log.i("QuestionViewAct:", e.getMessage());
            }

            finish();
        }
    }

    // 손이 터치하여 움직이는 부분을 전달할 수 있도록 socketchannel 연결을 진행하는 부분입니다.
    // 어떤 유저가 어떤 방에 들어가는지 전송합니다.
    public class connectSocket extends Thread{
        @Override
        public void run() {
            super.run();
            try {
                // 채팅 서버와 소켓 연결을 하는 부분입니다.
                socketChannel = SocketChannel.open();
                socketChannel.configureBlocking(true);
                socketChannel.connect(new InetSocketAddress(ip, port));

                // 소켓 연결된 서버에 내가 누구인지 알려줍니다.
                // 유저의 id와 서버에 접속한다는 의미인 type(join)을 채팅 서버로 전송합니다.
                jo.put("userId", sp.getString("id", ""));
                jo.put("roomId", queTitle);
                jo.put("type", "join");
                socketChannel.socket().getOutputStream().write(jo.toString().getBytes("EUC-KR"));

                Thread.sleep(1000);

                // 유저의 id와 서버에 방에 들어간다는 의미인 type(enter_room)을 채팅 서버로 전송합니다.
                jo.put("userId", sp.getString("id", ""));
                jo.put("type", "enter_room");

                socketChannel.socket().getOutputStream().write(jo.toString().getBytes("EUC-KR"));

                dv.receiveStart();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    // DB에 질문을 등록한다고 요청하는 부분입니다.
    public class updateQuestion extends AsyncTask<Void,String,String>{

        JSONObject jsonObject;

        updateQuestion(){
            jsonObject = new JSONObject();
        }

        @Override
        protected String doInBackground(Void... voids) {
            try{
                jsonObject.put("id", sp.getString("id", ""));
                jsonObject.put("title", queTitle);

                URL url = new URL("http://o-ddang.com/theteacher/updateQuestion.php");
                HttpURLConnection httpURLConnection = (HttpURLConnection)url.openConnection();

                httpURLConnection.setDefaultUseCaches(false);
                httpURLConnection.setDoInput(true);
                httpURLConnection.setDoOutput(true);
                httpURLConnection.setRequestMethod("POST");

                httpURLConnection.setRequestProperty("Accept", "application/json");
                httpURLConnection.setRequestProperty("Content-type", "application/json");

                OutputStream os = httpURLConnection.getOutputStream();
                os.write(jsonObject.toString().getBytes());
                os.flush();

                InputStreamReader tmp = new InputStreamReader(httpURLConnection.getInputStream(), "EUC-KR");
                BufferedReader reader = new BufferedReader(tmp);
                StringBuilder builder = new StringBuilder();
                String str;
                while ((str = reader.readLine()) != null) {
                    builder.append(str);
                }
                String result = builder.toString();
                return result;
            }  catch (MalformedURLException e) {
                e.printStackTrace();
                return e.getMessage();
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                return e.getMessage();
            } catch (ProtocolException e) {
                e.printStackTrace();
                return e.getMessage();
            } catch (IOException e) {
                e.printStackTrace();
                return e.getMessage();
            } catch (JSONException e) {
                e.printStackTrace();
                return e.getMessage();
            }
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            try {
                jsonObject = new JSONObject(s);
                String result = jsonObject.getString("process");
                if(result.equals("UpdateSuccess")){
                    Toast.makeText(getApplicationContext(), "질문을 등록하였습니다.", Toast.LENGTH_SHORT).show();
                } else if(result.equals("UpdateFailed")){
                    // 질문 등록에 실패할 경우 사진 url 등록된 부분까지 삭제할 수 있도록 진행합니다.
                    exitQuestion eq = new exitQuestion();
                    eq.start();
                    socketChannel.close();
                    finish();
                }
            } catch (JSONException e) {
                e.printStackTrace();
                Log.i("QuestionViewAct:", e.getMessage());
            } catch (IOException e) {
                e.printStackTrace();
                Log.i("QuestionViewAct:", e.getMessage());
            }
        }
    }
}
