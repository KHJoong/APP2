package com.example.theteacher;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

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
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by kimhj on 2018-02-08.
 */

public class LectureManageActivity extends AppCompatActivity {

    // 강의 제목을 입력하는 곳입니다.
    EditText etLecTitle;
    // 강의 목표를 입력하는 곳입니다.
    EditText etLecObj;
    // 강의에 대해 기타 설명하는 곳입니다.
    EditText etLecExplain;
    // 입력했던 내용을 다시 입력하고 싶을 경우 초기화 하는 버튼입니다.
    Button btnReset;
    // 입력한 내용을 서버에 등록하고 할 경우 누를 버튼입니다.
    Button btnEnroll;

    // 자신이 등록한 강의를 볼 수 있는 리스트뷰입니다.
    ListView lvLecList;
    Lecture_Adapter lectureAdapter;

    String lecTitle;
    String lecObject;
    String lecExplain;
    String lecTime;

    // LectureEnroll에서 id를 불러오기 위해 사용합니다.
    SharedPreferences sp;
    // SQLite에 등록한 강의를 저장합니다.
    DBHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.lecture_manage);

        sp = getSharedPreferences("profile", MODE_PRIVATE);

        etLecTitle = (EditText)findViewById(R.id.etLecTitle);
        etLecObj = (EditText)findViewById(R.id.etLecObj);
        etLecExplain = (EditText)findViewById(R.id.etLecExplain);

        btnReset = (Button)findViewById(R.id.btnReset);
        btnEnroll = (Button)findViewById(R.id.btnEnroll);

        lvLecList = (ListView)findViewById(R.id.lvLecList);
        lectureAdapter = new Lecture_Adapter(this);
        lvLecList.setAdapter(lectureAdapter);

        btnReset.setOnClickListener(btnClickListener);
        btnEnroll.setOnClickListener(btnClickListener);

        // 저장되있는 등록된 강의들을 불러오는 부분입니다.
        dbHelper = new DBHelper(this);
        dbHelper.selectLecture(lvLecList, lectureAdapter);
    }

    Button.OnClickListener btnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            switch (view.getId()){
                case R.id.btnReset :
                    EditTextReset();
                    break;
                case R.id.btnEnroll :
                    Date d = new Date(System.currentTimeMillis());
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd/HH/mm/ss");

                    lecTitle = etLecTitle.getText().toString();
                    lecObject = etLecObj.getText().toString();
                    lecExplain = etLecExplain.getText().toString();
                    lecTime = sdf.format(d);

                    JSONObject jo = new JSONObject();
                    try {
                        jo.put("id", sp.getString("id", ""));
                        jo.put("title", lecTitle);
                        jo.put("object", lecObject);
                        jo.put("explain", lecExplain);
                        jo.put("lasttime", lecTime);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    LectureEnroll le = new LectureEnroll(LectureManageActivity.this);
                    le.execute(jo.toString());
                    break;
            }
        }
    };

    // 모든 내용을 다시 입력하고자 할 때, EditText를 모두 비워주는 역할을 하는 함수입니다.
    public void EditTextReset(){
        etLecTitle.setText(null);
        etLecObj.setText(null);
        etLecExplain.setText(null);
    }

    // 버튼 클릭했을 경우 키보드를 숨겨주는 함수입니다.
    // LectureEnroll() 에서 성공적으로 강의를 등록했을 경우 발동합니다.
    public void dismissKeyboard(Activity activity) {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (null != activity.getCurrentFocus())
            imm.hideSoftInputFromWindow(activity.getCurrentFocus().getApplicationWindowToken(), 0);
    }

    // 입력한 강의를 서버에 전송합니다.
    // 성공적으로 서버에 등록되었을 경우 SQLite에도 저장하여 내 ListView에도 띄워줍니다.
    public class LectureEnroll extends AsyncTask<String, String, String>{

        Context leContext;

        // AsyncTask가 진행되는 동안 돌아갈 ProgressDialog입니다.
        ProgressDialog proDialog;
        // 서버에 강의 등록이 실패하면 띄워줄 AlertDialog입니다.
        AlertDialog.Builder alertDialogBuilder;

        LectureEnroll(Context context){
            leContext = context;

            alertDialogBuilder = new AlertDialog.Builder(context);

            proDialog = new ProgressDialog(context);
            proDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            proDialog.show();
        }

        @Override
        protected String doInBackground(String... params) {
            String sendData = params[0];
            try{
                URL url = new URL("http://www.o-ddang.com/theteacher/lectureEnroll.php");
                HttpURLConnection httpURLConnection = (HttpURLConnection)url.openConnection();

                httpURLConnection.setDefaultUseCaches(false);
                httpURLConnection.setDoInput(true);
                httpURLConnection.setDoOutput(true);
                httpURLConnection.setRequestMethod("POST");

                httpURLConnection.setRequestProperty("Accept", "application/json");
                httpURLConnection.setRequestProperty("Content-type", "application/json");

                OutputStream os = httpURLConnection.getOutputStream();
                os.write(sendData.getBytes());
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
            proDialog.dismiss();
            String result = null;
            try {
                JSONObject jo = new JSONObject(s);
                result = jo.getString("process");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            if(result.equals("EnrollSuccess")){
                dismissKeyboard(LectureManageActivity.this);

                dbHelper = new DBHelper(LectureManageActivity.this);
                dbHelper.insertLecture(lecTitle, lecObject, lecExplain, lecTime);

                EditTextReset();

                Lecture lecture = new Lecture(lecTitle, lecObject, lecExplain, lecTime);
                lectureAdapter.addItem(lecture);
            } else {
                // 서버에 강의 등록 실패 시 확인 알람 창 띄워줍니다.
                alertDialogBuilder.setMessage("네트워크 상태를 다시 확인해주세요.")
                        .setNeutralButton("OK", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        }).create().show();
            }
        }
    }

}
