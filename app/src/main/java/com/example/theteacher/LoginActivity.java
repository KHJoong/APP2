package com.example.theteacher;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
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

/**
 * Created by kimhj on 2018-01-23.
 * 이 클래스는 로그인이 안되어 있는 유저들을 위한 로그인 액티비티입니다.
 */

public class LoginActivity extends AppCompatActivity {

    EditText etLoginId;
    EditText etLoginPwd;
    Button btnLogin;
    Button btnMemberJoin;

    String id;
    String pwd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        etLoginId = (EditText)findViewById(R.id.etLoginId);
        etLoginPwd = (EditText)findViewById(R.id.etLoginPwd);
        btnLogin = (Button)findViewById(R.id.btnLogin);
        btnMemberJoin = (Button)findViewById(R.id.btnMemberJoin);

        btnLogin.setOnClickListener(btnClickListener);
        btnMemberJoin.setOnClickListener(btnClickListener);

    } // onCreate 끝나는 부분입니다.

    Button.OnClickListener btnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()){
                // 로그인 버튼을 눌렀을 때의 리스너입니다.
                case R.id.btnLogin:
                    id = etLoginId.getText().toString();
                    pwd = etLoginPwd.getText().toString();

                    if(TextUtils.isEmpty(id) && TextUtils.isEmpty(pwd)){
                        Toast.makeText(getApplicationContext(), "ID와 비밀번호를 입력해주세요.", Toast.LENGTH_SHORT).show();
                    } else if(TextUtils.isEmpty(id)) {
                        Toast.makeText(getApplicationContext(), "ID를 입력해주세요.", Toast.LENGTH_SHORT).show();
                    } else if(TextUtils.isEmpty(pwd)) {
                        Toast.makeText(getApplicationContext(), "비밀번호를 입력해주세요.", Toast.LENGTH_SHORT).show();
                    } else {
                        JSONObject sendJsonObj = new JSONObject();
                        try {
                            sendJsonObj.put("id", id);
                            sendJsonObj.put("pwd", pwd);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        LoginData ctask = new LoginData(LoginActivity.this);
                        ctask.execute(sendJsonObj.toString());
                    }
                    break;

                // 회원가입 버튼을 눌렀을 때의 리스너입니다.
                case R.id.btnMemberJoin:
                    Intent intent = new Intent(getApplicationContext(), MemberJoinActivity.class);
                    startActivity(intent);
                    break;
            }
        }
    };


    // 로그인을 위해 ID와 비밀번호를 서버로 전송하는 AsyncTask입니다.
    class LoginData extends AsyncTask<String, Void, String> {

        Context context;
        // AsyncTask가 진행되는 동안 돌아갈 ProgressDialog입니다.
        ProgressDialog proDialog;
        // 로그인 실패하면 띄워줄 AlertDialog입니다.
        AlertDialog.Builder alertDialogBuilder;

        public LoginData(Context con){
            context = con;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            alertDialogBuilder = new AlertDialog.Builder(context);

            proDialog = new ProgressDialog(context);
            proDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            proDialog.show();
        }

        @Override
        protected String doInBackground(String... params) {
            String logindata = params[0];
            Log.i("LoginAct:LoginData:", logindata);
            try{
                URL url = new URL("http://www.o-ddang.com/theteacher/memberLogin.php");
                HttpURLConnection httpURLConnection = (HttpURLConnection)url.openConnection();

                httpURLConnection.setDefaultUseCaches(false);
                httpURLConnection.setDoInput(true);
                httpURLConnection.setDoOutput(true);
                httpURLConnection.setRequestMethod("POST");

                httpURLConnection.setRequestProperty("Accept", "application/json");
                httpURLConnection.setRequestProperty("Content-type", "application/json");

                OutputStream os = httpURLConnection.getOutputStream();
                os.write(logindata.getBytes());
                os.flush();

                InputStreamReader tmp = new InputStreamReader(httpURLConnection.getInputStream(), "EUC-KR");
                BufferedReader reader = new BufferedReader(tmp);
                StringBuilder builder = new StringBuilder();
                String str;
                while ((str = reader.readLine()) != null) {
                    builder.append(str + "\n");
                }
                String result = builder.toString();
                Log.i("LoginAct:LoginData:", result);
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
            // 로그인의 결과로 서버에서 받아오는 String은
            // 성공 시 : isLogged="YES", id, sessionID, position 값이고
            // 실패 시 : isLogged="NO" 입니다.
            super.onPostExecute(s);
            Log.i("LoginAct:LoginData:", s);
            proDialog.dismiss();

            String isLogged = null;
            String id = null;
            String position = null;
            String sessionId = null;
            String picUri = null;

            try {
                JSONObject receiveJsonObj = new JSONObject(s);
                isLogged = receiveJsonObj.getString("isLogged");
                if(isLogged.equals("YES")){
                    id = receiveJsonObj.getString("id");
                    position = receiveJsonObj.getString("position");
                    sessionId = receiveJsonObj.getString("PHPSESSID");
                    picUri = "http://www.o-ddang.com/theteacher/"+receiveJsonObj.getString("picUrl");

                    SharedPreferences sp = getSharedPreferences("profile", MODE_PRIVATE);
                    SharedPreferences.Editor editor = sp.edit();
                    editor.putString("isLogged", isLogged);
                    editor.putString("id", id);
                    editor.putString("position", position);
                    editor.putString("sessionID", "PHPSESSID="+sessionId);
                    editor.putString("picUrl", picUri);
                    editor.commit();

                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                    startActivity(intent);
                    finish();

                } else if(isLogged.equals("NO")){
                    SharedPreferences sp = getSharedPreferences("profile", MODE_PRIVATE);
                    SharedPreferences.Editor editor = sp.edit();
                    editor.putString("isLogged", isLogged);
                    editor.commit();

                    // 로그인 실패 시 확인 알람 창 띄워줍니다.
                    alertDialogBuilder.setMessage("아이디 또는 패스워드를 다시 확인해주세요.")
                            .setNeutralButton("OK", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.cancel();
                                }
                            }).create().show();
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    } // LoginData AsyncTask 끝나는 부분입니다.

}
