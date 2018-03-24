package com.example.theteacher;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowManager;
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
 * Created by kimhj on 2018-03-22.
 */

// 사용자가 MainActivity_Recorded에서 소유하고 있는 비디오 파일을 업로드하는 과정에서 쓰이는 dialog입니다.
// 등록 버튼 클릭 > 비디오 파일을 선택 과정을 마친 후
// 업로드할 강의 제목을 정하기 위해 띄워주는 custom dialog입니다.
// 강의 제목을 입력받은 후 서버로 전송하고 비디오 데이터를 서버로 전송하기 위해 VideoSocketTransfer Thread class를 실행시킵니다.
public class RecordedEnroll_CustomDialog extends Dialog {

    // MainActivity_Recorded의 context입니다.
    Context reContext;

    SharedPreferences sp;

    // 제목을 입력받을 edittext입니다.
    EditText etRecordedTitle;
    // 제목을 입력한 후 등록을 시작할 때 클릭하는 버튼입니다.
    Button btnEnrollRecorded;

    public RecordedEnroll_CustomDialog(Context c){
        super(c, android.R.style.Theme_Translucent_NoTitleBar);
        reContext = c;
        sp = reContext.getSharedPreferences("profile", Context.MODE_PRIVATE);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 다이얼로그 외부 화면 흐리게 표현
        WindowManager.LayoutParams lpWindow = new WindowManager.LayoutParams();
        lpWindow.flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND;
        lpWindow.dimAmount = 0.8f;
        getWindow().setAttributes(lpWindow);

        setContentView(R.layout.recorded_enroll_dialog);

        etRecordedTitle = (EditText)findViewById(R.id.etRecordedTitle);
        btnEnrollRecorded = (Button)findViewById(R.id.btnEnrollRecorded);

        btnEnrollRecorded.setOnClickListener(btnClickListener);
    }

    Button.OnClickListener btnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()){
                case R.id.btnEnrollRecorded :
                    if(!TextUtils.isEmpty(etRecordedTitle.getText().toString())){
                        JSONObject jo = new JSONObject();
                        try {
                            jo.put("id", sp.getString("id",""));
                            jo.put("title", etRecordedTitle.getText().toString());
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        EnrollRecordedTitle ert = new EnrollRecordedTitle(reContext);
                        ert.execute(jo.toString());
                    } else {
                        Toast.makeText(reContext, "등록할 강의 제목을 입력해주세요.", Toast.LENGTH_SHORT).show();
                    }
                    break;
            }
        }
    };

    // 입력받은 강의 제목을 서버로 전송하는 역할을 합니다.
    // 서버로 전송이 완료되면 VideoSocketTransfer Thread class를 실행하여 비디오 파일을 서버로 보내도록 합니다.
    public class EnrollRecordedTitle extends AsyncTask<String, String, String>{

        Context ertContext;

        // AsyncTask가 진행되는 동안 돌아갈 ProgressDialog입니다.
        ProgressDialog proDialog;

        EnrollRecordedTitle(Context context){
            ertContext = context;

            proDialog = new ProgressDialog(context);
            proDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            proDialog.show();
        }

        @Override
        protected String doInBackground(String... params) {
            String sendData = params[0];
            try{
                URL url = new URL("http://www.o-ddang.com/theteacher/recordedEnroll.php");
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

            String result = null;
            String repath = null;
            try {
                JSONObject getJsonData = new JSONObject(s);
                result = getJsonData.getString("process");
                if(result.equals("success")){
                    repath = getJsonData.getString("repath");

                    // 소켓연결해서 서버(VideoReceiver)로 비디오 파일을 보내는 부분
//                    VideoSocketTransfer vst = new VideoSocketTransfer(proDialog, repath, MainActivity_Recorded.videoPath);
//                    vst.start();
                    Intent socketIntent = new Intent(reContext, VideoTransferService.class);
                    socketIntent.putExtra("videoTitle", repath);
                    socketIntent.putExtra("videoPath", MainActivity_Recorded.videoPath);
                    reContext.startService(socketIntent);

                    proDialog.dismiss();
                    dismiss();
                } else {
                    Toast.makeText(reContext, "네트워크 상태를 확인한 후 다시 시도해주세요.", Toast.LENGTH_SHORT).show();
                    proDialog.dismiss();
                    dismiss();
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }





}
