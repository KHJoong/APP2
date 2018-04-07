package com.example.theteacher;

import android.annotation.TargetApi;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import static android.app.Activity.RESULT_OK;

/**
 * Created by kimhj on 2018-03-22.
 */

public class MainActivity_Recorded extends Fragment implements AbsListView.OnScrollListener{

    ListView lvRecordedPlaying;
    Recorded_Adapter recordedAdapter;
    FloatingActionButton fabEnroll;

    SharedPreferences sp;

    // Video File을 선택하는 Intent를 실행한 후 결과를 Return 받기 위해 사용되는 코드입니다.
    private final static int SELECT_VIDEO_CODE = 7777;

    // 선택한 video의 Uri를 담을 변수입니다.
    // content:// 로 시작하는 주소를 얻습니다.
    Uri videoUri;
    // 선택한 video의 Uri를 이용하여 실제 경로를 담을 변수입니다.
    // /storage/emulate/0/Picture......... 와 같은 경로를 얻습니다.
    static String videoPath;

    // 지금 진행중인 강의를 불러올 때 페이징하여 불러오기 위해 사용하는 변수입니다.
    // Activity가 실행될(onCreate) 때 0으로 시작해서 새로운 강의 목록을 요청할 때마다 1씩 증가합니다.
    // 보낼 때 10를 곱해서 보닙니다.(앞에 받아온 개수만큼 offset 할 수 있도록)
    int pagingNum;
    // listview position에 따라 다음 페이지 아이템들을 요청해야 하는지 아닌지 결정해주는 변수입니다.
    // 요청중일때는 false, 요청이 끝나면 true로 설정됩니다.(1번만 요청하도록 설정하기 위해서 사용하는 변수입니다.)
    // 따라서 기본값은 false고, true일 때만 요청을 보낼 수 있습니다.
    boolean isPaging;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.main_recorded, container, false);

        pagingNum = 0;
        isPaging = false;

        lvRecordedPlaying = (ListView) view.findViewById(R.id.lvRecordedLecture);
        fabEnroll = (FloatingActionButton)view.findViewById(R.id.fabEnroll);
        recordedAdapter = new Recorded_Adapter(getActivity().getApplicationContext());

        fabEnroll.setOnClickListener(btnClickListener);

        lvRecordedPlaying.setAdapter(recordedAdapter);
        lvRecordedPlaying.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // 리스트뷰의 아이템을 클릭하게되면 해당 강의를 시청하는 화면으로 이동합니다.
                // 아이템마다 저장되어 있는 파일의 이름(repath)을 RecordedViewActivity에 전달하여 url 주소로 사용할 수 있도록 합니다.
                Intent it = new Intent(getActivity().getApplicationContext(), RecordedViewActivity.class);
                it.putExtra("repath", recordedAdapter.raItem.get(position).getRecordedPath());
                startActivity(it);
            }
        });

        StartGetRecorded();

        return view;
    }

    Button.OnClickListener btnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()){
                case R.id.fabEnroll:
                    Toast.makeText(getActivity(), "등록할 동영상을 선택해주세요.", Toast.LENGTH_SHORT).show();
                    selectVideo();
                    break;
            }
        }
    };

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {

    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        // 현재 listview에서 확인하지 않은 아이템이 5개 이하가되면 다음 목록을 준비해둘 수 있도록 합니다.
        int count = totalItemCount - 5;
        if(firstVisibleItem>=count && isPaging == true){
            StartGetRecorded();
            // isPaging이 true이기 때문에 다음 페이지를 요청하는 함수를 실행한 후 완료될 때까지 재요청하지 않도록 isPaging 값을 false로 바꿔둡니다.
            isPaging = false;
        }
    }

    // fabEnroll 버튼을 클릭했을 때 실행되는 함수입니다.
    // Video 파일 목록을 띄워주는 Intent를 실행합니다.
    public void selectVideo(){
        Intent intent = new Intent();
        intent.setType("video/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select a Video "), SELECT_VIDEO_CODE);
    }

    // onActivityResult에서 실행되는 함수입니다.
    // content://....... 와 같은 Uri를 이용하여 /storage/emulate/0/.......같은 실제 path를 얻는 데 사용됩니다.
    public String getVideoPath(Uri u){
        String filePath = null;
        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
        if(isKitKat){
            filePath = generateFromKitkat(u,getActivity());
        }

        if(filePath != null){
            return filePath;
        }

        Cursor cursor = getActivity().getContentResolver().query(u, new String[] { MediaStore.MediaColumns.DATA }, null, null, null);

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
                filePath = cursor.getString(columnIndex);
            }
            cursor.close();
        }
        return filePath == null ? u.getPath() : filePath;
    }

    // 위의 getVideoPath에서 사용되는 함수입니다.
    // 만약 기기의 OS 버전이 kitkat 이상이면 ContentResolver를 이용하여 이 부분에서 실제 path를 받습니다.
    @TargetApi(19)
    private String generateFromKitkat(Uri uri,Context context){
        String filePath = null;
        if(DocumentsContract.isDocumentUri(context, uri)){
            String wholeID = DocumentsContract.getDocumentId(uri);

            String id = wholeID.split(":")[1];

            String[] column = { MediaStore.Video.Media.DATA };
            String sel = MediaStore.Video.Media._ID + "=?";

            Cursor cursor = context.getContentResolver().query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, column, sel, new String[]{ id }, null);

            int columnIndex = cursor.getColumnIndex(column[0]);

            if (cursor.moveToFirst()) {
                filePath = cursor.getString(columnIndex);
            }

            cursor.close();
        }
        return filePath;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_OK){
            if(requestCode == SELECT_VIDEO_CODE){
                // 선택한 Video의 Uri를 받고, 그것으로 Path를 찾아내는 부분입니다.
                videoUri = null;
                videoPath = null;
                videoUri = data.getData();
                videoPath = getVideoPath(videoUri);

                // 파일의 크기가 2GB를 넘으면 등록할 수 없도록 추가
                File video = new File(videoPath);
                long length = video.length();
                if(length/1024/1024/1024>=2){
                    Toast.makeText(getActivity().getApplicationContext(), "파일의 크기가 2GB를 넘을 수 없습니다.", Toast.LENGTH_SHORT).show();
                } else {
                    // 등록할 강의의 제목을 입력받기 위해 custom dialog를 띄워줍니다.
                    RecordedEnroll_CustomDialog reDialog = new RecordedEnroll_CustomDialog(getActivity());
                    reDialog.setCancelable(true);
                    reDialog.getWindow().setGravity(Gravity.CENTER);
                    reDialog.show();
                }
                video = null;
            }
        }
    }

    // 서버에 저장되어 있는 녹화 강의를 불러오는 첫 과정입니다.
    // 이 함수에서는 id와 paging 수(몇 개를 받아왔는지)를 보낼 수 있도록 Json 형태로 묶어준 후 getRecLecture를 실행시킵니다.
    public void StartGetRecorded(){
        sp = getActivity().getSharedPreferences("profile", Context.MODE_PRIVATE);

        JSONObject jo = new JSONObject();
        try {
            jo.put("id", sp.getString("id", ""));
            jo.put("paging", 10*pagingNum);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        getRecLecture grl = new getRecLecture(getActivity(), lvRecordedPlaying, recordedAdapter);
        grl.execute(jo.toString());
    }

    // 실질적으로 서버에 녹화 강의를 불러오는 부분입니다.
    // 불러온 후 Listview에 등록하여 화면에 보이도록 합니다.
    public class getRecLecture extends AsyncTask<String, String, String>{

        Context grlContext;
        ListView lvRecorded;
        Recorded_Adapter recorded_adapter;

        // AsyncTask가 진행되는 동안 돌아갈 ProgressDialog입니다.
        ProgressDialog proDialog;
        // 서버에서 강의 불러오는 것이 실패하면 띄워줄 AlertDialog입니다.
        AlertDialog.Builder alertDialogBuilder;

        String path, id, title, repath;

        getRecLecture(Context con, ListView lv, Recorded_Adapter ra){
            grlContext = con;
            lvRecorded = lv;
            recorded_adapter = ra;

            alertDialogBuilder = new AlertDialog.Builder(grlContext);

            proDialog = new ProgressDialog(getActivity());
            proDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            proDialog.show();
        }

        @Override
        protected String doInBackground(String... params) {
            String sendData = params[0];
            try{
                URL url = new URL("http://www.o-ddang.com/theteacher/recordedLectureList.php");
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

        // 서버로부터 성공하면 process의 value 값으로 LodingSuccess를 받고 recorded의 value 값으로 강의에 대한 정보가 담겨잇는 array를 받습니다.
        // recorded의 array마다 path(강사 프로필 사진 경로), id(업로드 유저 id), title(강의 제목), repath(저장되어있는 파일 경로)가 담겨있습니다.
        // path, id, title, repath > 4개를 listview의 아이템에 담아둡니다.
        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            proDialog.dismiss();
            String result = null;
            try {
                JSONObject jo1 = new JSONObject(s);
                result = jo1.getString("process");
                JSONArray array = jo1.getJSONArray("recorded");

                if(result.equals("LodingSuccess")){
                    if(array != null){
                        for(int n=0; n<array.length(); n++){
                            String joLec = array.getString(n);
                            JSONObject jo = new JSONObject(joLec);

                            if(!TextUtils.isEmpty(jo.getString("path"))){
                                path = "http://www.o-ddang.com/theteacher/" +jo.getString("path");
                            } else {
                                path = "http://www.o-ddang.com/theteacher/base_profile_img.png";
                            }
                            if(!TextUtils.isEmpty(jo.getString("id"))){
                                id = jo.getString("id");
                            }
                            if(!TextUtils.isEmpty(jo.getString("title"))){
                                title = jo.getString("title");
                            }
                            if(!TextUtils.isEmpty(jo.getString("repath"))){
                                repath = jo.getString("repath");
                            }
                            // true가 의미하는 것은 없습니다.
                            // 생성자를 만들때 4개 필요한 경우가 중복되어 하나 더 넣었습니다.
                            Lecture lecture = new Lecture(path, title, id, repath, true);
                            recorded_adapter.addItem(lecture);
                        }
                        recorded_adapter.notifyDataSetChanged();

                        // 리스너가 한 번만 등록되도록 설정해주는 부분입니다.
                        // 처음에만 등록하면 되기 떄문에 pagingNum이 0일때만 한 번 실행되도록 합니다.
                        if(pagingNum==0){
                            lvRecorded.setOnScrollListener(MainActivity_Recorded.this);
                        }

                        pagingNum++;
                        isPaging = true;
                    } else {
                        isPaging = false;
                    }
                } else {
                    // 서버에서 강의 로딩 실패 시 확인 알람 창 띄워줍니다.
                    alertDialogBuilder.setMessage("네트워크 상태를 다시 확인해주세요.")
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
    }



}
