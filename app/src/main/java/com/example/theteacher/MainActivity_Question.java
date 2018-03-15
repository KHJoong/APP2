package com.example.theteacher;

import android.app.Activity;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Created by kimhj on 2018-03-12.
 */

public class MainActivity_Question extends Fragment implements AbsListView.OnScrollListener{

    // 이 Bitmap은 DrawingView에서 배경 사진으로 설정하기 위해 사용됩니다.
    // 서버에 질문하기 위한 사진을 등록한 후 그 사진을 bitmap으로 저장해두고 DrawingView에서 사용합니다.
    static Bitmap mBitmap;
    // DrawingView에서 손으로 터치한 부분의 좌표를 상대방에게 전달할 때 화면의 크기 대비 %로 변환하여 전달하기 위해 계산할 때 쓰입니다.
    static int qWidth;
    static int qHeight;

    // onActivityResult() 를 위해 쓰이는 코드입니다.
    // 크롭했을 때의 코드입니다.
    private final static int TAKE_CROP_CODE = 5555;
    // 카메라로 찍을 때의 코드입니다.
    private final static int TAKE_CAMERA_CODE = 1111;
    // 갤러리를 이용했을 때의 코드입니다.
    private final static int TAKE_GALLERY_CODE = 3333;

    Question_Adapter qAdapter;
    GridView gvQuestion;

    // 질문을 등록하기 위한 버튼입니다.
    // 클릭할 경우 카메라로 사진을 찍을지, 갤러리에서 가져올지 선택하게됩니다.
    FloatingActionButton fabEnroll;

    // 바로 위의 fabEnroll을 클릭하면 나타나는 custom dialog입니다.
    QuestionEnroll_CustomDialog qeDialog;

    // 카메라로 찍던, 갤러리에서 불러오던 사진의 경로를 저장해주는 두 변수입니다.
    String currentPicPath;
    Uri picUri;

    SharedPreferences sp;

    // 지금 진행중인 강의를 불러올 때 페이징하여 불러오기 위해 사용하는 변수입니다.
    // Activity가 실행될(onCreate) 때 0으로 시작해서 새로운 강의 목록을 요청할 때마다 1씩 증가합니다.
    // 보낼 때 10를 곱해서 보닙니다.(앞에 받아온 개수만큼 offset 할 수 있도록)
    int pagingNum;
    // listview position에 따라 다음 페이지 아이템들을 요청해야 하는지 아닌지 결정해주는 변수입니다.
    // 요청중일때는 false, 요청이 끝나면 true로 설정됩니다.(1번만 요청하도록 설정하기 위해서 사용하는 변수입니다.)
    // 따라서 기본값은 false고, true일 때만 요청을 보낼 수 있습니다.
    boolean isPaging;

    // 클릭한 질문(item)의 질문 제목을 저장해두기 위한 변수입니다.
    String queTitle;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.main_question, container, false);

        pagingNum = 0;
        isPaging = false;

        sp = getActivity().getSharedPreferences("profile", Context.MODE_PRIVATE);

        fabEnroll = (FloatingActionButton)view.findViewById(R.id.fabEnroll);
        gvQuestion = (GridView) view.findViewById(R.id.gvQeestion);
        qAdapter = new Question_Adapter(getActivity().getApplicationContext());
        gvQuestion.setAdapter(qAdapter);

        gvQuestion.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ProgressDialog proDialog = new ProgressDialog(getActivity());
                proDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                proDialog.show();

                queTitle = qAdapter.qItem.get(position).getQuestionTitle();

                downloadPic dp = new downloadPic(proDialog, qAdapter.qItem.get(position).getQuestionPicUrl(), "other");
                dp.start();
            }
        });

        fabEnroll.setOnClickListener(btnClickListener);

        // 처음 화면을 볼 때 기존에 등록되어 있는 질문들을 불러오기 위한 함수입니다.
        startGetQuestion();

        return view;
    }

    View.OnClickListener btnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()){
                case R.id.fabEnroll:
                    qeDialog = new QuestionEnroll_CustomDialog(getActivity(), goCameraLis, goGalleryLis);
                    qeDialog.setCancelable(true);
                    qeDialog.getWindow().setGravity(Gravity.CENTER);
                    qeDialog.show();
                    break;
            }
        }
    };

    // 이 버튼을 클릭하면 사진을 찍는 프로세스가 실행됩니다.
    View.OnClickListener goCameraLis = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            TakePicCameraIntent();
        }
    };

    // 이 버튼을 클릭하면 갤러리에서 사진을 불러오는 프로세스가 진행됩니다.
    View.OnClickListener goGalleryLis = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            TakePicGalleryIntent();
        }
    };

    // onScrollStateChanged, onScroll는 GridView(gvQuestion)이 스크롤 될 때 사용됩니다.
    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {

    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        // 현재 listview에서 확인하지 않은 아이템이 2개 이하가되면 다음 목록을 준비해둘 수 있도록 합니다.
        int count = totalItemCount - 2;
        if(firstVisibleItem+visibleItemCount>=count && isPaging == true){
            startGetQuestion();

            // isPaging이 true이기 때문에 다음 페이지를 요청하는 함수를 실행한 후 완료될 때까지 재요청하지 않도록 isPaging 값을 false로 바꿔둡니다.
            isPaging = false;
        }
    }

    // 카메라로 사진 찍는 버튼을 클릭할 경우 실행되는 함수입니다.
    public void TakePicCameraIntent(){
        Intent takePicCameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        File photoFile = null;
        try{
            // 새로 찍은 사진 파일의 이름을 지정해주는 부분입니다.
            // 찍은 시간을 이름으로 합니다.
            photoFile = CreateImageFile();
        } catch (IOException e){
            Toast.makeText(getActivity().getApplicationContext(),"이미지 처리 오류! 다시 시도해주세요.", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }

        if (photoFile != null) {
            // Provider를 통해 photoFile 경로에 TheTeacher의 provider가 사진을 저장하겠다고 말해주는 부분입니다..
            picUri = FileProvider.getUriForFile(getActivity().getApplicationContext(),"com.example.theteacher.provider", photoFile);
            takePicCameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, picUri);
            startActivityForResult(takePicCameraIntent, TAKE_CAMERA_CODE);
        }

    }

    // 사진을 찍거나, 수정한 시간으로 jpg 파일 이름을 정합니다.
    // TakePicCameraIntent(), CropPic() 에서 사용됩니다.
    public File CreateImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File storageDir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/TheTeacher/");
        // 위에서 설정한 폴더가 없을 경우 생성해주는 부분입니다.
        if (!storageDir.exists()) {
            storageDir.mkdirs();
        }
        File image = File.createTempFile(timeStamp, ".jpg", storageDir);

        currentPicPath = image.getAbsolutePath();

        return image;
    }

    // 갤러리에서 사진을 선택하겠다고 요청할 경우 실행되는 함수입니다.
    public void TakePicGalleryIntent(){
        Intent takePicGalleryIntent = new Intent(Intent.ACTION_PICK);
        takePicGalleryIntent.setData(MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(takePicGalleryIntent, TAKE_GALLERY_CODE);
    }

    // 사진을 찍거나, 갤러리에서 선택한 후 크롭하는 역할을 해주는 부분입니다.
    // 누가 버전부터 작업을 진행하기 전에 FLAG를 이용하여 쓰기, 읽기 권한을 주고 uri를 활용해야 합니다.
    public void CropPic(){
        // Crop이 실행되기 전에 사전 setting 값을 설정할 수 있도록 권한을 설정해주는 부분입니다.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            getActivity().grantUriPermission("com.android.camera", picUri,
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
        Intent intent = new Intent("com.android.camera.action.CROP");
        intent.setDataAndType(picUri, "image/*");

        List<ResolveInfo> list = getActivity().getPackageManager().queryIntentActivities(intent, 0);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            getActivity().grantUriPermission(list.get(0).activityInfo.packageName, picUri,
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
        int size = list.size();
        if (size == 0) {
            Toast.makeText(getActivity().getApplicationContext(), "취소 되었습니다.", Toast.LENGTH_SHORT).show();
            return;
        } else {
            Toast.makeText(getActivity().getApplicationContext(), "용량이 큰 사진의 경우 시간이 오래 걸릴 수 있습니다.", Toast.LENGTH_SHORT).show();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            }
            intent.putExtra("crop", "true");
            intent.putExtra("aspectX", 1);
            intent.putExtra("aspectY", 1);
            intent.putExtra("scale", true);
            File croppedFileName = null;
            try {
                // 잘라낸 후 저장될 사진의 이름을 받아오는 부분입니다.
                croppedFileName = CreateImageFile();
            } catch (IOException e) {
                e.printStackTrace();
            }

            File folder = new File(Environment.getExternalStorageDirectory() + "/TheTeacher/");
            File tempFile = new File(folder.toString(), croppedFileName.getName());

            // Provider를 통해 photoFile 경로에 TheTeacher의 provider가 사진을 저장하겠다고 말해주는 부분입니다..
            picUri = FileProvider.getUriForFile(getActivity().getApplicationContext(),"com.example.theteacher.provider", tempFile);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            }

            intent.putExtra("return-data", false);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, picUri);
            intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());

            Intent i = new Intent(intent);
            ResolveInfo res = list.get(0);
            // Crop이 실행된 후에도 Uri를 활용할 수 있도록 하기 위해 권한을 설정해주는 부분입니다.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                i.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

                getActivity().grantUriPermission(res.activityInfo.packageName, picUri,
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }
            i.setComponent(new ComponentName(res.activityInfo.packageName, res.activityInfo.name));
            startActivityForResult(i, TAKE_CROP_CODE);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode==Activity.RESULT_OK){
            if(requestCode==TAKE_CAMERA_CODE){
                CropPic();
                // 갤러리에 나타나게
                MediaScannerConnection.scanFile(getActivity().getApplicationContext(),
                        new String[]{picUri.getPath()}, null,
                        new MediaScannerConnection.OnScanCompletedListener() {
                            public void onScanCompleted(String path, Uri uri) {
                            }
                        });
            } else if(requestCode==TAKE_CROP_CODE){
                // 유저가 선택한 사진을 서버로 업로드하여 다른 사람이 볼 수 있도록 준비시켜두는 부분입니다.
                UpLoadQuestionPic ulqp = new UpLoadQuestionPic(getActivity(), currentPicPath);
                ulqp.execute();
            } else if(requestCode==TAKE_GALLERY_CODE){
                if (data == null) {
                    return;
                }
                picUri = data.getData();

                CropPic();
            }
        }
    }

    // 서버에 등록되어있는 질문 목록을 요청하기 위해서 id와 pagingNum을 JSONObject에 담는 역할을 하는 함수입니다.
    // JSONObject에 담은 후 LoadQuestion() 실행하여 요청까지 하는 함수입니다.
    public void startGetQuestion(){
        // 서버로 보낼 id를 JSON에 담는 부분입니다.
        sp = getActivity().getSharedPreferences("profile", Context.MODE_PRIVATE);
        JSONObject jo = new JSONObject();
        try {
            jo.put("id", sp.getString("id", ""));
            jo.put("paging", 10*pagingNum);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        LoadQuestion lq = new LoadQuestion(getActivity(), gvQuestion, qAdapter);
        lq.execute(jo.toString());
    }

    // 실질적으로 서버에 등록된 질문들을 요청하는 부분입니다.
    // 불러온 후 GridView(gvQuestion)에 등록합니다.
    // 아이템은 등록된 시간 기준 내림차순으로, 질문 사진 url과 title을 가지고 등록됩니다.
    public class LoadQuestion extends AsyncTask<String, String, String>{

        Context lqContext;
        GridView gvQuestion;
        Question_Adapter lqAdapter;

        String qPath;
        String qTitle;

        // AsyncTask가 진행되는 동안 돌아갈 ProgressDialog입니다.
        ProgressDialog proDialog;
        // 사진 업로드에 실패하면 띄워줄 AlertDialog입니다.
        AlertDialog.Builder alertDialogBuilder;

        LoadQuestion(Context context, GridView gv, Question_Adapter qa){
            lqContext = context;
            gvQuestion = gv;
            lqAdapter = qa;

            alertDialogBuilder = new AlertDialog.Builder(context);
            proDialog = new ProgressDialog(context);
            proDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            proDialog.show();
        }

        @Override
        protected String doInBackground(String... params) {
            String sendData = params[0];
            try{
                URL url = new URL("http://www.o-ddang.com/theteacher/getQuestion.php");
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
                            qPath = jo.getString("path");
                        }
                        if(!TextUtils.isEmpty(jo.getString("title"))){
                            qTitle = jo.getString("title");
                        }
                        Question question = new Question(qPath, qTitle);
                        lqAdapter.addItem(question);
                    }
                    lqAdapter.notifyDataSetChanged();

                    // 리스너가 한 번만 등록되도록 설정해주는 부분입니다.
                    // 처음에만 등록하면 되기 떄문에 pagingNum이 0일때만 한 번 실행되도록 합니다.
                    if(pagingNum==0){
                        gvQuestion.setOnScrollListener(MainActivity_Question.this);
                    }

                    pagingNum++;
                    isPaging = true;
                } else if(result.equals("LodingEnd")) {
                    // 모든 질문이 다 불러졌으면 더이상 요청하지 않도록 false로 고정합니다.
                    isPaging = false;
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

    // 질문을 등록했을 때 서버로 질문의 내용인 사진을 등록하는 부분입니다.
    public class UpLoadQuestionPic extends AsyncTask<Void, String, String>{

        // AsyncTask가 진행되는 동안 돌아갈 ProgressDialog입니다.
        ProgressDialog proDialog;
        // 사진 업로드에 실패하면 띄워줄 AlertDialog입니다.
        AlertDialog.Builder alertDialogBuilder;

        Context uqContext;
        String filePath;
        File sourceFile;

        String lineEnd = "\r\n";
        String twoHypens = "--";
        String boundary = "*****";

        int bytesRead, bytesAvailable, bufferSize;
        byte[] buffer;
        int maxBufferSize = 20480;

        public UpLoadQuestionPic(Context context, String uploadFilePath){
            alertDialogBuilder = new AlertDialog.Builder(context);
            proDialog = new ProgressDialog(context);
            proDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            proDialog.show();

            uqContext = context;
            filePath = uploadFilePath;
            sourceFile = new File(filePath);
        }

        @Override
        protected String doInBackground(Void... Void) {
            SharedPreferences sp = getActivity().getSharedPreferences("profile", Context.MODE_PRIVATE);
            String sessionID = sp.getString("sessionID", "");
            try{
                FileInputStream fis = new FileInputStream(sourceFile);
                URL url = new URL("http://www.o-ddang.com/theteacher/uploadQuestionPic.php");

                HttpURLConnection conn = (HttpURLConnection)url.openConnection();
                conn.setDoInput(true);
                conn.setDoOutput(true);
                conn.setUseCaches(false);
                conn.setRequestMethod("POST");

                conn.setInstanceFollowRedirects( false );
                if(!TextUtils.isEmpty(sessionID)) {
                    conn.setRequestProperty( "cookie", sessionID) ;
                }
                conn.setRequestProperty("Connection", "Keep-Alive");
                conn.setRequestProperty("ENCTYPE", "multipart/form-data");
                conn.setRequestProperty("Content-Type", "multipart/form-data;boundary="+boundary);
                conn.setRequestProperty("uploaded_file", filePath);

                DataOutputStream dos = new DataOutputStream(conn.getOutputStream());

                dos.writeBytes(twoHypens+boundary+lineEnd);
                dos.writeBytes("Content-Disposition: form-data; name=\"uploaded_image\""+lineEnd);
                dos.writeBytes(lineEnd);
                dos.writeBytes("uploaded_question_pic");
                dos.writeBytes(lineEnd);

                dos.writeBytes(twoHypens+boundary+lineEnd);
                dos.writeBytes("Content-Disposition: form-data; name=\"userId\";"+lineEnd);
                dos.writeBytes(lineEnd);
                dos.writeBytes(sp.getString("id", ""));
                dos.writeBytes(lineEnd);

                dos.writeBytes(twoHypens+boundary+lineEnd);
                dos.writeBytes("Content-Disposition: form-data; name=\"uploaded_file\"; filename=\""+filePath+"\""+lineEnd);
                dos.writeBytes(lineEnd);

                bytesAvailable = fis.available();

                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                buffer = new byte[bufferSize];

                bytesRead = fis.read(buffer, 0, bufferSize);

                while (bytesRead > 0) {
                    dos.write(buffer, 0, bufferSize);
                    bytesAvailable = fis.available();
                    bufferSize = Math.min(bytesAvailable, maxBufferSize);
                    bytesRead = fis.read(buffer, 0, bufferSize);
                }

                dos.writeBytes(lineEnd);
                dos.writeBytes(twoHypens+boundary+lineEnd);

                int responseStatusCode = conn.getResponseCode();

                InputStream inputStream;
                if(responseStatusCode == HttpURLConnection.HTTP_OK) {
                    inputStream = conn.getInputStream();
                } else{
                    inputStream = conn.getErrorStream();
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

                fis.close();
                dos.flush();
                dos.close();

                return result;
            } catch (Exception e) {
                Log.i("MainAct_Question:","UpLoadQuestionPic:"+e.toString());
                return "fail";
            }
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            String[] resultArray = s.split("/");
            String result = resultArray[8]+"/"+resultArray[9];
            String picUrl = null;
            try {
                JSONObject getJsonData = new JSONObject(result);
                result = getJsonData.getString("process");
                picUrl = getJsonData.getString("picUrl");

                // 서버로 사진 전송이 완료되면 Bitmap에 저장할 수 있도록 해주는 Thread를 실행시킵니다..
                // DrawingView에서 사용할 Bitmap입니다.
                downloadPic dp = new downloadPic(proDialog, picUrl, "my");
                dp.start();
            } catch (JSONException e) {
                e.printStackTrace();
            }
            if(result.equals("UploadSuccess")){

            } else {
                // 사진 업로드 실패 시 확인 알람 창 띄워줍니다.
                alertDialogBuilder.setMessage("사진 혹은 네트워크 상태를 다시 확인해주세요.")
                        .setNeutralButton("OK", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        }).create().show();
            }
        }
    }

    // QuestionViewActivity의 DrawingView에서 배경으로 사용할 Bitmap을 저장하는 부분입니다.
    // bitmap 형식으로 서버에서 받아옵니다.
    // 성공적으로 변환이 완료되면 QuestionViewActivity를 실행시킵니다.
    public class downloadPic extends Thread{

        ProgressDialog progressDialog;

        String url;
        String type;

        downloadPic(ProgressDialog pd, String u, String t){
            progressDialog = pd;
            url = "http://o-ddang.com/theteacher/"+u;
            type = t;
            mBitmap = null;
        }

        @Override
        public void run() {
            super.run();
            try {
                DisplayMetrics displayMetrics = new DisplayMetrics();
                getActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
                qWidth = displayMetrics.widthPixels;
                qHeight = displayMetrics.heightPixels;

                Drawable dr = Glide.with(getActivity()).load(url).into(qWidth,qHeight).get();
                if (dr instanceof BitmapDrawable) {
                    BitmapDrawable bitmapDrawable = (BitmapDrawable) dr;
                    if(bitmapDrawable.getBitmap() != null) {
                        mBitmap = bitmapDrawable.getBitmap();
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
            progressDialog.dismiss();
            if(qeDialog!=null){
                qeDialog.dismiss();
            }
            Intent intent = new Intent(getActivity(), QuestionViewActivity.class);
            intent.putExtra("picUrl", url);
            intent.putExtra("type", type);
            if(!TextUtils.isEmpty(queTitle)){
                intent.putExtra("title", queTitle);
            }
            startActivity(intent);
        }
    }


}
