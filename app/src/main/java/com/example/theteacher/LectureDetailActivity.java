package com.example.theteacher;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.channels.SocketChannel;

/**
 * Created by kimhj on 2018-02-10.
 */

public class LectureDetailActivity extends AppCompatActivity {

    ImageView ivPic;
    TextView tvId;
    TextView tvTitle;
    TextView tvObject;
    TextView tvExplain;
    Button btnVideoCall;

    String path;
    String id;
    String title;
    String object;
    String explain;

    SharedPreferences sp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.lecture_detail);

        sp = getSharedPreferences("profile", MODE_PRIVATE);

        ivPic = (ImageView) findViewById(R.id.ivPic);
        tvId = (TextView) findViewById(R.id.tvId);
        tvTitle = (TextView) findViewById(R.id.tvTitle);
        tvObject = (TextView) findViewById(R.id.tvObject);
        tvExplain = (TextView) findViewById(R.id.tvExplain);
        btnVideoCall= (Button) findViewById(R.id.btnVideoCall);

        btnVideoCall.setOnClickListener(btnClickListener);

        // MainActivity_Home로부터 받은 Intent에 담겨있는 정보입니다.
        // 클릭한 강의의 세부 정보가 담겨 있습니다.
        // 위에서부터 순서대로
        // path는 강사 설정 사진 URL, id는 강사 id, title은 강의 제목, object는 강의 목표, explain은 강의 설명 입니다.
        Intent getDataFromHome = getIntent();
        path = getDataFromHome.getStringExtra("path");
        id = getDataFromHome.getStringExtra("id");
        title = getDataFromHome.getStringExtra("title");
        object = getDataFromHome.getStringExtra("object");
        explain = getDataFromHome.getStringExtra("explain");

        Glide.with(this).load(path).into(ivPic);
        tvId.setText(id);
        tvTitle.setText(title);
        tvObject.setText(object);
        tvExplain.setText(explain);
    }

    Button.OnClickListener btnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            switch (view.getId()){
                case R.id.btnVideoCall :
                    JSONObject jo = new JSONObject();
                    try {
                        // 버튼을 클릭할 경우 지금 보고있는 강의의 강사 ID를 담아서 서버로 보냅니다.
                        // 서버에서 해당 강사의 ID에 맞는 socket channel을 찾은 후 신호를 전달해줍니다.
                        // type의 signal은 서버에 연결되어 있을 때, 신호를 요청하는 중이라는 뜻입니다.
                        jo.put("userId", sp.getString("id", ""));
                        jo.put("type", "signal");
                        jo.put("toUserId", id);
                        final String sendData = jo.toString();
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    SocketService.socketChannel.socket().getOutputStream().write(sendData.getBytes("EUC-KR"));
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }).start();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    break;
            }
        }
    };
}
