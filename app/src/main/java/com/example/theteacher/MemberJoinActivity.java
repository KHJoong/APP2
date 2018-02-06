package com.example.theteacher;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by kimhj on 2018-01-23.
 * 이 클래스는 처음 설치한 유저들의 회원가입을 위한 액티비티입니다.
 */

public class MemberJoinActivity extends AppCompatActivity {

    EditText etId;
    EditText etPwd;
    EditText etPwdCheck;
    TextView tvPwdAlert;
    EditText etEmail;
    RadioButton rbTeacher;
    RadioButton rbStudent;
    Button btnOk;
    Button btnCancel;

    String id;          // 클라이언트 ID
    String pwd;         // 클라이언트 비밀번호
    String email;       // 클라이언트 email 주소
    String position;    // 클라이언트 직책 선택(선생님 or 학생)
    String joinDate;    // 클라이언트 회원 가입 시간

    int pwdCheck;       // 클라이언트 비밀번호 확인 > 일치하면 확인 버튼 클릭 시 정상적으로 진행될 수 있게 체크하는 정수(btnOk의 클릭리스너 참고)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.member_join);

        etId = (EditText)findViewById(R.id.etId);
        etPwd = (EditText)findViewById(R.id.etPwd);
        etPwdCheck = (EditText)findViewById(R.id.etPwdCheck);
        tvPwdAlert = (TextView)findViewById(R.id.tvPwdAlert);
        etEmail = (EditText)findViewById(R.id.etEmail);
        rbTeacher = (RadioButton)findViewById(R.id.rbTeacher);
        rbStudent = (RadioButton)findViewById(R.id.rbStudent);
        btnOk = (Button)findViewById(R.id.btnOk);
        btnCancel = (Button)findViewById(R.id.btnCancel);

        btnOk.setOnClickListener(btnClickListener);
        btnCancel.setOnClickListener(btnClickListener);

        rbTeacher.setOnClickListener(rbClickListener);
        rbStudent.setOnClickListener(rbClickListener);

        pwdCheck = 0;
        etPwdCheck.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                pwd = etPwd.getText().toString();
                if(pwd.equals(etPwdCheck.getText().toString())){
                    tvPwdAlert.setText("입력하신 비밀번호가 일치합니다.");
                    pwdCheck = 1;
                } else {
                    tvPwdAlert.setText("위의 비밀번호와 같지 않습니다.");
                    tvPwdAlert.setTextColor(Color.RED);
                    pwdCheck = 0;
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

    }   // onCreate 끝부분입니다.

    RadioButton.OnClickListener rbClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()){
                // 클라이언트가 선택한 직책이 선생님인 경우
                case R.id.rbTeacher:
                    position = "teacher";
                    break;

                // 클라이언트가 선택한 직책이 학생인 경우
                case R.id.rbStudent:
                    position = "student";
                    break;
            }
        }
    };

    Button.OnClickListener btnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()){

                // 회원가입 확인 버튼이 클릭되면 발생되는 이벤트입니다.
                // 서버로 id, pwd, email, 직책, 가입 시간을 JSON 형태로 변환하여 전송합니다.
                case R.id.btnOk:
                    Date d = new Date(System.currentTimeMillis());
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
                    id = etId.getText().toString();
                    pwd = etPwd.getText().toString();
                    email = etEmail.getText().toString();
                    joinDate = sdf.format(d);

                    // id, pwd, email, position이 비어있으면 입력을 요청합니다.
                    // pwdCheck 변수는 TextWatcher에서 유저가 입력한 비밀번호와 비밀번호 확인이 일치한다고 판단되면 1, 불일치는 0을 담고 있습니다.
                    // 따라서 pwdCheck 변수가 0이면 확인하도록 유도합니다.
                    if(TextUtils.isEmpty(id)){
                        Toast.makeText(getApplicationContext(), "ID를 입력해주세요", Toast.LENGTH_SHORT).show();
                    } else if(TextUtils.isEmpty(pwd)){
                        Toast.makeText(getApplicationContext(), "비밀번호를 입력해주세요", Toast.LENGTH_SHORT).show();
                    } else if(pwdCheck == 0){
                        Toast.makeText(getApplicationContext(), "비밀번호 확인하지 않으셨거나 두 비밀번호가 일치하지 않습니다.", Toast.LENGTH_SHORT).show();
                    } else if(TextUtils.isEmpty(email)){
                        Toast.makeText(getApplicationContext(), "Email을 입력해주세요", Toast.LENGTH_SHORT).show();
                    } else if(TextUtils.isEmpty(position)){
                        Toast.makeText(getApplicationContext(), "직책을 골라주세요", Toast.LENGTH_SHORT).show();
                    } else {
                        JSONObject sendJsonObj = new JSONObject();
                        try {
                            sendJsonObj.put("id", id);
                            sendJsonObj.put("pwd", pwd);
                            sendJsonObj.put("email", email);
                            sendJsonObj.put("position", position);
                            sendJsonObj.put("joinDate", joinDate);

                            JoinData joinData = new JoinData(MemberJoinActivity.this);
                            joinData.execute(sendJsonObj.toString());
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                    break;

                // 회원가입을 취소합니다.
                case R.id.btnCancel:
                    finish();
                    break;
            }
        }
    };


    // 서버로 회원가입 요청한 데이터를 전송하는 AsyncTask입니다.
    // id, 비밀번호, email, 직책, 가입날짜를 전송하고
    // 성공하면 sessionid 값, 실패하면 string "failed"를 받아옵니다.
    class JoinData extends AsyncTask<String, Void, String> {

        Context context;
        ProgressDialog proDialog;

        public JoinData(Context con){
            context = con;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            proDialog = new ProgressDialog(context);
            proDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            proDialog.show();
        }

        @Override
        protected String doInBackground(String... params) {
            String data = params[0];
            Log.i("MemberJoinAct:JoinData:", data);
            try{
                URL url = new URL("http://o-ddang.com/theteacher/memberJoin.php");
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
                Log.i("MemberJoinAct:JoinData:", result);
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
            Log.i("MemberJoinAct:JoinData:", s);
            String processResult = null;
            String userId = null;
            try {
                JSONObject jo = new JSONObject(s);
                userId = jo.getString("id");
                processResult = jo.getString("success");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            proDialog.dismiss();
            if(processResult.equals("success")){
                Toast.makeText(getApplicationContext(), "ID : "+userId+" 님의 회원가입이 완료되었습니다.", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(getApplicationContext(), "회원가입에 실패하였습니다. 잠시 후 다시 시도해주세요.", Toast.LENGTH_SHORT).show();
            }
        }
    } // JoinData AsyncTask 끝나는 부분입니다.










}
