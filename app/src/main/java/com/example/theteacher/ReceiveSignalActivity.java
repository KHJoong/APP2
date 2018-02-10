package com.example.theteacher;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by kimhj on 2018-02-10.
 */

public class ReceiveSignalActivity extends AppCompatActivity {

    TextView tvId;
    ImageView ivPic;
    Button btnCallOk;
    Button btnCallNo;

    String otherId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.receive_signal);

        tvId = (TextView) findViewById(R.id.tvId);
        ivPic = (ImageView) findViewById(R.id.ivPic);
        btnCallOk = (Button) findViewById(R.id.btnCallOk);
        btnCallNo = (Button) findViewById(R.id.btnCallNo);

        btnCallOk.setOnClickListener(btnClickListener);
        btnCallNo.setOnClickListener(btnClickListener);

        Intent getFromSockService = getIntent();
        otherId = getFromSockService.getStringExtra("id");
        tvId.setText(otherId);

        JSONObject jo = new JSONObject();
        try {
            jo.put("id", otherId);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // 상담을 요청한 유저의 프로필 사진을 요청받는 AsyncTask입니다.
        // 상대방 유저가 업로드한 사진이 있을 경우 그 사진의 경로를
        // 업로드한 사진이 없을 경우 기본 사진(base_profile_img.png)를 ivPic에 띄웁니다.
        GetInquireUserPic giup = new GetInquireUserPic(this);
        giup.execute(jo.toString());
    }

    Button.OnClickListener btnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            switch (view.getId()){
                case R.id.btnCallOk :

                    break;
                case R.id.btnCallNo :
                    finish();
                    break;
            }
        }
    };

    // 상담을 요청한 유저의 사진 경로를 요청하는 AsyncTask입니다.
    // 상대방 id를 전송합니다.
    // 받아온 경로를 이용하여 ivPic에 띄웁니다.
    class GetInquireUserPic extends AsyncTask<String, Void, String> {

        Context giupContext;

        public GetInquireUserPic(Context con){
            giupContext = con;
        }

        @Override
        protected String doInBackground(String... params) {
            String data = params[0];
            Log.i("ReceiveSignalAct:", "GetInquireUserPic:"+data);
            try{
                URL url = new URL("http://o-ddang.com/theteacher/inquireUserPic.php");
                HttpURLConnection httpURLConnection = (HttpURLConnection)url.openConnection();

                httpURLConnection.setDefaultUseCaches(false);
                httpURLConnection.setDoInput(true);
                httpURLConnection.setDoOutput(true);
                httpURLConnection.setRequestMethod("POST");

                httpURLConnection.setRequestProperty("Accept", "application/json");
                httpURLConnection.setRequestProperty("Content-type", "application/json");

                OutputStream os = httpURLConnection.getOutputStream();
                os.write(data.getBytes());
                os.flush();

                InputStreamReader tmp = new InputStreamReader(httpURLConnection.getInputStream(), "EUC-KR");
                BufferedReader reader = new BufferedReader(tmp);
                StringBuilder builder = new StringBuilder();
                String str;
                while ((str = reader.readLine()) != null) {
                    builder.append(str + "\n");
                }
                String result = builder.toString();
                Log.i("ReceiveSignalAct:", "GetInquireUserPic:"+result);
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
            // 회원가입의 결과로 서버에서 받아오는 String은 성공시 사용자의 id, 실패시 "failed"입니다.
            super.onPostExecute(s);
            Log.i("ReceiveSignalAct:", "GetInquireUserPic:"+s);
            String path = null;
            String realPath = null;
            try {
                JSONObject jo = new JSONObject(s);
                path = jo.getString("path");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            if(!TextUtils.isEmpty(path)){
                realPath = "http://www.o-ddang.com/theteacher/"+path;
                Glide.with(giupContext).load(realPath).into(ivPic);
            } else {
                realPath = "http://www.o-ddang.com/theteacher/base_profile_img.png";
                Glide.with(giupContext).load(realPath).into(ivPic);
            }
        }
    } // JoinData AsyncTask 끝나는 부분입니다.
}
