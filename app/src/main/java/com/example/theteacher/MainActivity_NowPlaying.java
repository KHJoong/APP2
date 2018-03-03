package com.example.theteacher;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AlertDialog;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by kimhj on 2018-03-01.
 */

public class MainActivity_NowPlaying extends Fragment implements AbsListView.OnScrollListener{

    SharedPreferences sp;

    ListView lvNowPlaying;
    NowPlaying_Adapter nowPlayingAdapter;

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
        View view = inflater.inflate(R.layout.main_nowplaying, container, false);

        pagingNum = 0;
        isPaging = false;

        lvNowPlaying = (ListView) view.findViewById(R.id.lvNowPlaying);
        nowPlayingAdapter = new NowPlaying_Adapter(getActivity());

        lvNowPlaying.setAdapter(nowPlayingAdapter);
        lvNowPlaying.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // 리스트뷰의 아이템을 클릭하게되면 해당 강의를 시청하는 화면으로 이동합니다.
                // 강사의 id를 강의 시청 화면으로 넘겨서 어떤 강의를 보는지 알 수 있도록 합니다.
                // 전달된 teacherId는 시청할 강의의 rtsp url을 확인하는데 사용됩니다.
                Intent it = new Intent(getActivity().getApplicationContext(), LectureViewActivity.class);
                it.putExtra("teacherId", nowPlayingAdapter.npItem.get(position).getTeacherId());
                startActivity(it);
            }
        });

        startNowPlayingLecture(); // 성공적으로 목록을 받아왔을 경우 isPaging을 true로 설정해주는 부분이 getNowPlayingLecture Asynctask에 있습니다.

        return view;
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {

    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        // 현재 listview에서 확인하지 않은 아이템이 5개 이하가되면 다음 목록을 준비해둘 수 있도록 합니다.
        int count = totalItemCount - 5;
        if(firstVisibleItem>=count && isPaging == true){
            startNowPlayingLecture();
            // isPaging이 true이기 때문에 다음 페이지를 요청하는 함수를 실행한 후 완료될 때까지 재요청하지 않도록 isPaging 값을 false로 바꿔둡니다.
            isPaging = false;
        }
    }

    // 강의가 시작한지 몇 분이 지났는지 계산해주는 함수입니다.
    // 현재 시간을 구하고 서버에서 받아온 각 강의의 시작 시간과의 차이를 계산합니다.
    // 리턴값은 long이지만 int > String의 변환을 거쳐 TextView에 등록됩니다.
    public long getDiffTime(String getday){
        long diff = 0;
        long min = 0;
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy/MM/dd/HH/mm/ss");
        try {
            Date tdate = new Date(System.currentTimeMillis());
//            String tmpdate = simpleDateFormat.format(date);
//
//            Date tdate = simpleDateFormat.parse(tmpdate);
            Date gdate = simpleDateFormat.parse(getday);
            diff = tdate.getTime()-gdate.getTime();
            min = diff/1000/60;
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return min;
    }

    // 서버에 새로운 강의 목록을 요청하기 위해서 id와 pagingNum을 JSONObject에 담는 역할을 하는 함수입니다.
    // JSONObject에 담은 후 getNewLecture() 실행하여 요청까지 하는 함수입니다.
    public void startNowPlayingLecture(){
        // 서버로 보낼 id를 JSON에 담는 부분입니다.
        sp = getActivity().getSharedPreferences("profile", Context.MODE_PRIVATE);
        JSONObject jo = new JSONObject();
        try {
            jo.put("id", sp.getString("id", ""));
            jo.put("paging", 10*pagingNum);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        getNowPlayingLecture gnpl = new getNowPlayingLecture(getActivity().getApplicationContext(), lvNowPlaying, nowPlayingAdapter);
        gnpl.execute(jo.toString());
    }

    // 서버에서 현재 진행중인 강의 목록을 실질적으로 요청하는 AsyncTask입니다.
    // id와 paging 구분 변수를 전송합니다.
    public class getNowPlayingLecture extends AsyncTask<String, String, String> {

        Context gnplContext;
        ListView lvNowPlayingLec;
        NowPlaying_Adapter npAdapter;

        // AsyncTask가 진행되는 동안 돌아갈 ProgressDialog입니다.
        ProgressDialog proDialog;
        // 서버에서 강의 불러오는 것이 실패하면 띄워줄 AlertDialog입니다.
        AlertDialog.Builder alertDialogBuilder;

        String path, id, title, lasttime;
        int difftime;

        getNowPlayingLecture(Context context, ListView lv, NowPlaying_Adapter npa){
            gnplContext = context;
            lvNowPlayingLec = lv;
            npAdapter = npa;

            alertDialogBuilder = new AlertDialog.Builder(context);

            proDialog = new ProgressDialog(getActivity());
            proDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            proDialog.show();
        }

        @Override
        protected String doInBackground(String... params) {
            String sendData = params[0];
            try{
                URL url = new URL("http://www.o-ddang.com/theteacher/nowPlayingLecture.php");
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

        // 결과에는 강사의 id, 강의 제목, thumbnail 경로, 강의를 시작한 시간이 들어있습니다.
        // 강의 시작 시간이 최근일수록 리스트뷰의 맨 위에 위치합니다.
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
                        if(!TextUtils.isEmpty(jo.getString("id"))){
                            id = jo.getString("id");
                        }
                        if(!TextUtils.isEmpty(jo.getString("title"))){
                            title = jo.getString("title");
                        }
                        if(!TextUtils.isEmpty(jo.getString("path"))){
                            path = "http://www.o-ddang.com/theteacher/"+jo.getString("path");
                        }
                        if(!TextUtils.isEmpty(jo.getString("lasttime"))){
                            lasttime = jo.getString("lasttime");
                            difftime = (int)(long)getDiffTime(lasttime);
                        }
                        Lecture lecture = new Lecture(path, id, title, String.valueOf(difftime),1);
                        npAdapter.addItem(lecture);
                    }
                    npAdapter.notifyDataSetChanged();

                    // 리스너가 한 번만 등록되도록 설정해주는 부분입니다.
                    // 처음에만 등록하면 되기 떄문에 pagingNum이 0일때만 한 번 실행되도록 합니다.
                    if(pagingNum==0){
                        lvNowPlayingLec.setOnScrollListener(MainActivity_NowPlaying.this);
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
