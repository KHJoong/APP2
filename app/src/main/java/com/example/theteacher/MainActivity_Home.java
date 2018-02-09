package com.example.theteacher;

import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ListView;

import org.json.JSONArray;
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
 * Created by kimhj on 2018-01-25.
 */

public class MainActivity_Home extends Fragment implements AbsListView.OnScrollListener{

    ListView lvNewLecture;
    NewLecture_Adapter newLectureAdapter;

    SharedPreferences sp;

    // 새로운 강의를 불러올 때 페이징하여 불러오기 위해 사용하는 변수입니다.
    // Activity가 실행될(onCreate) 때 0으로 시작해서 새로운 강의 목록을 요청할 때마다 1씩 증가합니다.
    // 보낼 때 10를 곱해서 보닙니다.(앞에 받아온 개수만큼 offset 할 수 있도록)
    int pagingNum;
    // listview position에 따라 다음 페이지 아이템들을 요청해야 하는지 아닌지 결정해주는 변수입니다.
    // 요청중일때는 false, 요청이 끝나면 true로 설정됩니다.(1번만 요청하도록 설정하기 위해서 사용하는 변수입니다.)
    // 따라서 true일 때만 요청을 보낼 수 있습니다.
    boolean isPaging;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.main_home, container, false);

        pagingNum = 0;
        isPaging = false;

        lvNewLecture = (ListView) view.findViewById(R.id.lvNewLecture);
        newLectureAdapter = new NewLecture_Adapter(getActivity());

        lvNewLecture.setAdapter(newLectureAdapter);

        startGetNewLecture(); // 성공적으로 목록을 받아왔을 경우 isPaging을 true로 설정해주는 부분이 getNewLecture Asynctask에 있습니다.

        return view;
    }

    @Override
    public void onScrollStateChanged(AbsListView absListView, int i) {

    }

    @Override
    public void onScroll(AbsListView absListView, int firstvisibleitem, int visibleitemcount, int totalitemcount) {
        // 현재 listview에서 확인하지 않은 아이템이 5개 이하가되면 다음 목록을 준비해둘 수 있도록 합니다.
        int count = totalitemcount - 5;
        if(firstvisibleitem>=count && isPaging == true){
            startGetNewLecture();
            // isPaging이 true이기 때문에 다음 페이지를 요청하는 함수를 실행한 후 완료될 때까지 재요청하지 않도록 isPaging 값을 false로 바꿔둡니다.
            isPaging = false;
        }
    }

    // 서버에 새로운 강의 목록을 요청하기 위해서 id와 pagingNum을 JSONObject에 담는 역할을 하는 함수입니다.
    // JSONObject에 담은 후 getNewLecture() 실행하여 요청까지 하는 함수입니다.
    public void startGetNewLecture(){
        isPaging = true;

        // 서버로 보낼 id를 JSON에 담는 부분입니다.
        sp = getActivity().getSharedPreferences("profile", Context.MODE_PRIVATE);
        JSONObject jo = new JSONObject();
        try {
            jo.put("id", sp.getString("id", ""));
            jo.put("paging", 10*pagingNum);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        getNewLecture gnl = new getNewLecture(getActivity().getApplicationContext(), lvNewLecture, newLectureAdapter);
        gnl.execute(jo.toString());
    }

    // 서버에 새로운 강의 목록을 실질적으로 요청하는 AsyncTask입니다.
    public class getNewLecture extends AsyncTask<String, String, String>{

        Context gnlContext;
        ListView lvNewLec;
        NewLecture_Adapter nlAdap;
        // AsyncTask가 진행되는 동안 돌아갈 ProgressDialog입니다.
        ProgressDialog proDialog;
        // 서버에서 강의 불러오는 것이 실패하면 띄워줄 AlertDialog입니다.
        AlertDialog.Builder alertDialogBuilder;

        String path;
        String id;
        String title;
        String object;
        String explain;
        String lasttime;

        getNewLecture(Context context, ListView lv, NewLecture_Adapter nla){
            gnlContext = context;
            lvNewLec = lv;
            nlAdap = nla;

            alertDialogBuilder = new AlertDialog.Builder(context);

            proDialog = new ProgressDialog(getActivity());
            proDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            proDialog.show();
        }

        @Override
        protected String doInBackground(String... params) {
            String sendData = params[0];
            try{
                URL url = new URL("http://www.o-ddang.com/theteacher/newLectureList.php");
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
                JSONObject jo1 = new JSONObject(s);
                result = jo1.getString("process");
                JSONArray array = jo1.getJSONArray("lecture");

                if(result.equals("LodingSuccess")){

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
                        if(!TextUtils.isEmpty(jo.getString("object"))){
                            object = jo.getString("object");
                        }
                        if(!TextUtils.isEmpty(jo.getString("explain"))){
                            explain = jo.getString("explain");
                        }
                        if(!TextUtils.isEmpty(jo.getString("lasttime"))){
                            lasttime = jo.getString("lasttime");
                        }
                        Lecture lecture = new Lecture(path, id, title, object, explain, lasttime);
                        nlAdap.addItem(lecture);
                    }
                    lvNewLec.setAdapter(nlAdap);
                    lvNewLec.setSelection(10*pagingNum);

                    if(pagingNum==0){
                        lvNewLec.setOnScrollListener(MainActivity_Home.this);
                    }

                    pagingNum++;
                    isPaging = true;
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
