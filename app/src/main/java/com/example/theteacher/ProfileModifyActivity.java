package com.example.theteacher;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by kimhj on 2018-01-25.
 */

public class ProfileModifyActivity extends AppCompatActivity {

    // onActivityResult() 를 위해 쓰이는 코드입니다.
    // 크롭했을 때의 코드입니다.
    private final static int CROP_CODE = 5555;
    // 카메라로 찍을 때의 코드입니다.
    private final static int CAMERA_CODE = 1111;
    // 갤러리를 이용했을 때의 코드입니다.
    private final static int GALLERY_CODE = 3333;

    // 새로 입력한 비밀번호화 비밀번호 확인이 일치한지 여부를 담고있는 변수입니다.
    // 일치하면 1 , 일치하지 않으면 0을 담습니다.
    int pwdCheck = 0;

    private static String currentPhotoPath;
    private static Uri photoUri;

    Button btnCamera;           // 킄릭하면 카메라로 프로필 사진을 찍습니다.
    Button btnGallery;          // 클릭하면 캘러리에서 프로필 사진을 가져옵니다.
    Button btnPicRemove;        // 클릭하면 기존에 등록되어 있든 프로필 사진을 삭제합니다.
    ImageView ivProfilePic;     // 현재 프로필 사진을 띄워줍니다.
    TextView tvMyId;            // 자신의 ID를 담고 있는 뷰입니다.
    EditText etExPwd;           // 현재 패스워드를 입력합니다.(패스워드 변경 시 입력해야 하는 부분입니다.)
    EditText etNewPwd;          // 바꿀 패스워드를 입력합니다.
    EditText etNewPwdCheck;     // 바꿀 패스워드를 한 번 더 입력합니다.
    TextView tvPwdAlert;        // etNewPwd, etNewPwdCheck가 일치하지 않을 경우 경고를 띄워주는 텍스트뷰입니다.
    Button btnProfileChange;    // 정보를 모두 입력했을 때 변경을 요청하는 버튼입니다.

    // 메모리 읽기와 쓰기, 카메라의 권한을 체크할 때 쓸 배열입니다.
    // permissionCheck() 에서 사용됩니다.
    private String[] permissions = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA};
    private static final int MULTIPLE_PERMISSIONS = 101;

    SharedPreferences sp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.profile_modify);

        permissionCheck();

        sp = getSharedPreferences("profile", MODE_PRIVATE);

        btnCamera = (Button) findViewById(R.id.btnCamera);
        btnGallery = (Button) findViewById(R.id.btnGallery);
        btnPicRemove = (Button) findViewById(R.id.btnPicRemove);
        ivProfilePic = (ImageView) findViewById(R.id.ivProfilePic);
        tvMyId = (TextView) findViewById(R.id.tvMyId);
        etExPwd = (EditText) findViewById(R.id.etExPwd);
        etNewPwd = (EditText) findViewById(R.id.etNewPwd);
        etNewPwdCheck = (EditText) findViewById(R.id.etNewPwdCheck);
        tvPwdAlert = (TextView)findViewById(R.id.tvPwdAlert);
        btnProfileChange = (Button) findViewById(R.id.btnProfileChange);

        String picUrl = sp.getString("picUrl", null);

        if(!TextUtils.isEmpty(picUrl)){
            Glide.with(this).load(picUrl).into(ivProfilePic);
        } else {
            Glide.with(this).load(R.drawable.base_profile_img).into(ivProfilePic);
        }

        etNewPwdCheck.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String pwd = etNewPwd.getText().toString();
                if(pwd.equals(etNewPwdCheck.getText().toString())){
                    tvPwdAlert.setText("입력하신 비밀번호가 일치합니다.");
                    tvPwdAlert.setTextColor(Color.GRAY);
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

        btnCamera.setOnClickListener(btnClickListener);
        btnGallery.setOnClickListener(btnClickListener);
        btnPicRemove.setOnClickListener(btnClickListener);
        btnProfileChange.setOnClickListener(btnClickListener);
    }


    Button.OnClickListener btnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()){
                case R.id.btnCamera:
                    TakePicCameraIntent();
                    break;
                case R.id.btnGallery:
                    TakePicGalleryIntent();
                    break;
                case R.id.btnPicRemove:
                    RemovePic();
                    break;
                case R.id.btnProfileChange:
                    if(!TextUtils.isEmpty(etExPwd.getText().toString())){
                        if(pwdCheck==1){
                            String exPwd = etExPwd.getText().toString();
                            String newPwd = etNewPwd.getText().toString();
                            ChangeProfile(exPwd, newPwd);
                        }
                    } else {
                        Toast.makeText(ProfileModifyActivity.this, "비밀번호를 입력해주세요.", Toast.LENGTH_SHORT).show();
                    }
                    break;
            }
        }
    };

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        finish();
    }

    // permissions 배열에 들어있는 메모리 읽기 쓰기, 카메라 권한 중 설정되지 않은 항목이 있는지 체크하는 부분입니다.
    private boolean permissionCheck() {
        int result;
        List<String> permissionList = new ArrayList<>();
        for (String pm : permissions) {
            result = ContextCompat.checkSelfPermission(this, pm);
            if (result != PackageManager.PERMISSION_GRANTED) {
                permissionList.add(pm);
            }
        }
        if (!permissionList.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissionList.toArray(new String[permissionList.size()]), MULTIPLE_PERMISSIONS);
            return false;
        }
        return true;
    }

    // permissionCheck() 에서 권한을 요청한 것에 대해 결과를 받아와 처리하는 부분입니다.
    // 승인했을 경우 그냥 진행 되지만, 거부했을 경우 showNoPermissionToastAndFinish()이 실행되며 종료됩니다.
    // 다시 권한을 요청하게됩니다.
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MULTIPLE_PERMISSIONS: {
                if (grantResults.length > 0) {
                    for (int i = 0; i < permissions.length; i++) {
                        if (permissions[i].equals(this.permissions[0])) {
                            if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                                // 읽기 권한
                                showNoPermissionToastAndFinish();
                            }
                        } else if (permissions[i].equals(this.permissions[1])) {
                            if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                                // 쓰기 권한
                                showNoPermissionToastAndFinish();

                            }
                        } else if (permissions[i].equals(this.permissions[2])) {
                            if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                                // 카메라 권한
                                showNoPermissionToastAndFinish();

                            }
                        }
                    }
                } else {
                    showNoPermissionToastAndFinish();
                }
                return;
            }
        }
    }

    private void showNoPermissionToastAndFinish() {
        Toast.makeText(this, "권한 요청에 동의 해주셔야 이용 가능합니다. 설정에서 권한 허용 하시기 바랍니다.", Toast.LENGTH_SHORT).show();
        finish();
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
            Toast.makeText(this,"이미지 처리 오류! 다시 시도해주세요.", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }

        if (photoFile != null) {
            // Provider를 통해 photoFile 경로에 TheTeacher의 provider가 사진을 저장하겠다고 말해주는 부분입니다..
            photoUri = FileProvider.getUriForFile(this,"com.example.theteacher.provider", photoFile);
            takePicCameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
            startActivityForResult(takePicCameraIntent, CAMERA_CODE);
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

        currentPhotoPath = image.getAbsolutePath();

        return image;
    }

    // 사진을 찍거나, 갤러리에서 선택한 후 크롭하는 역할을 해주는 부분입니다.
    // 누가 버전부터 작업을 진행하기 전에 FLAG를 이용하여 쓰기, 읽기 권한을 주고 uri를 활용해야 합니다.
    public void CropPic(){
        // Crop이 실행되기 전에 사전 setting 값을 설정할 수 있도록 권한을 설정해주는 부분입니다.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            this.grantUriPermission("com.android.camera", photoUri,
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
        Intent intent = new Intent("com.android.camera.action.CROP");
        intent.setDataAndType(photoUri, "image/*");

        List<ResolveInfo> list = getPackageManager().queryIntentActivities(intent, 0);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            grantUriPermission(list.get(0).activityInfo.packageName, photoUri,
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
        int size = list.size();
        if (size == 0) {
            Toast.makeText(this, "취소 되었습니다.", Toast.LENGTH_SHORT).show();
            return;
        } else {
            Toast.makeText(this, "용량이 큰 사진의 경우 시간이 오래 걸릴 수 있습니다.", Toast.LENGTH_SHORT).show();

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
            photoUri = FileProvider.getUriForFile(this,"com.example.theteacher.provider", tempFile);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            }

            intent.putExtra("return-data", false);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
            intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());

            Intent i = new Intent(intent);
            ResolveInfo res = list.get(0);
            // Crop이 실행된 후에도 Uri를 활용할 수 있도록 하기 위해 권한을 설정해주는 부분입니다.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                i.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

                grantUriPermission(res.activityInfo.packageName, photoUri,
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }
            i.setComponent(new ComponentName(res.activityInfo.packageName, res.activityInfo.name));
            startActivityForResult(i, CROP_CODE);
        }
    }

    // 갤러리에서 사진을 선택하겠다고 요청할 경우 실행되는 함수입니다.
    public void TakePicGalleryIntent(){
        Intent takePicGalleryIntent = new Intent(Intent.ACTION_PICK);
        takePicGalleryIntent.setData(MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(takePicGalleryIntent, GALLERY_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode==RESULT_OK){
            if(requestCode==CAMERA_CODE){
                CropPic();
                // 갤러리에 나타나게
                MediaScannerConnection.scanFile(this,
                        new String[]{photoUri.getPath()}, null,
                        new MediaScannerConnection.OnScanCompletedListener() {
                            public void onScanCompleted(String path, Uri uri) {
                            }
                        });
            } else if(requestCode==CROP_CODE){
                // 유저가 선택한 사진을 Imageview에 매핑하고, 서버에도 업로드하는 함수입니다.
                savePhoto();
            } else if(requestCode==GALLERY_CODE){
                if (data == null) {
                    return;
                }
                photoUri = data.getData();

                CropPic();
            }
        }
    }

    // 유저가 선택한 사진을 서버로 보내는 AsyncTask를 실행시킵니다.
    // 성공적으로 서버에 등록되면 ImageView에 그 사진을 띄웁니다.
    // 실패할 경우 AlertDialog를 띄워줍니다.
    public void savePhoto(){
        UploadPic uploadPic = new UploadPic(this, currentPhotoPath);
        uploadPic.execute();
    }

    // 유저가 업로드했던 사진을 삭제하는 AsyncTask입니다.
    // 성공할 경우 서버에 업로드했던 사진의 경로를 삭제하고 Imageview를 기본 이미지 사진으로 바꿉니다.
    public void RemovePic(){
        RemovePic removePic = new RemovePic(this);
        removePic.execute();
    }

    public void ChangeProfile(String Ex, String New){
        String exP = Ex;
        String newP = New;

        JSONObject jo = new JSONObject();
        try {
            jo.put("id", sp.getString("id",""));
            jo.put("exP",exP);
            jo.put("newP", newP);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        String send = jo.toString();

        ChangeProfile cp = new ChangeProfile(this);
        cp.execute(send);
    }

    // 유저가 선택한 사진을 서버로 보내기 위한 AsyncTask입니다.
    // 업로드 성공시 : ImageView에 그 사진 매핑
    // 업로드 실패시 : AlertDialog로 실패 알림
    // onActivityResult()에서 requestCode가 CROP_CODE인 경우 savePhoto()가 실행되고 그 안에서 이 Async가 시작됩니다.
    public class UploadPic extends AsyncTask<String, String, String>{

        // AsyncTask가 진행되는 동안 돌아갈 ProgressDialog입니다.
        ProgressDialog proDialog;
        // 사진 업로드에 실패하면 띄워줄 AlertDialog입니다.
        AlertDialog.Builder alertDialogBuilder;

        Context upContext;
        String filePath;
        File sourceFile;

        String lineEnd = "\r\n";
        String twoHypens = "--";
        String boundary = "*****";

        int bytesRead, bytesAvailable, bufferSize;
        byte[] buffer;
        int maxBufferSize = 20480;

        public UploadPic(Context context, String uploadFilePath){
            alertDialogBuilder = new AlertDialog.Builder(context);
            proDialog = new ProgressDialog(context);
            proDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            proDialog.show();

            upContext = context;
            filePath = uploadFilePath;
            sourceFile = new File(filePath);
        }

        @Override
        protected String doInBackground(String... strings) {
            String sessionID = sp.getString("sessionID", "");
            try{
                FileInputStream fis = new FileInputStream(sourceFile);
                URL url = new URL("http://www.o-ddang.com/theteacher/uploadPic.php");

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
                dos.writeBytes("uploaded_pic");
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
                    Log.i("ProfileModifyAct:", "UploadPic:"+str);
                }
                String result = builder.toString();

                fis.close();
                dos.flush();
                dos.close();

                return result;
            } catch (Exception e) {
                Log.i("ProfileModifyAct:","UploadPic:"+e.toString());
                return "fail";
            }
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            proDialog.dismiss();
            String result = null;
            String picUrl = null;
            try {
                JSONObject getJsonData = new JSONObject(s);
                result = getJsonData.getString("process");
                picUrl = getJsonData.getString("picUrl");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            if(result.equals("UploadSuccess")){
                Glide.with(upContext).load(photoUri).into(ivProfilePic);
                SharedPreferences.Editor spEditor = sp.edit();
                spEditor.putString("picUrl", "http://www.o-ddang.com/theteacher/"+picUrl);
                spEditor.commit();

                Toast.makeText(upContext, "사진 파일 업데이트에 성공하였습니다.", Toast.LENGTH_SHORT).show();
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

    // 유저가 선택했던 사진을 삭제하는 AsyncTask입니다.
    // 삭제를 요청할 경우 서버에 업로드한 유저의 사진의 경로를 삭제합니다.
    // 다른 사람이 이 유저의 사진은 기본 사진을 보게 됩니다.
    // RemovePic() 에서 불러집니다.
    public class RemovePic extends AsyncTask<Void, String, String>{

        Context rpContext;

        // AsyncTask가 진행되는 동안 돌아갈 ProgressDialog입니다.
        ProgressDialog proDialog;
        // 사진 삭제에 실패하면 띄워줄 AlertDialog입니다.
        AlertDialog.Builder alertDialogBuilder;

        public RemovePic(Context context){
            rpContext = context;

            alertDialogBuilder = new AlertDialog.Builder(context);
            proDialog = new ProgressDialog(context);
            proDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            proDialog.show();
        }

        @Override
        protected String doInBackground(Void... voids) {
            JSONObject jo = new JSONObject();
            try {
                jo.put("id", sp.getString("id",""));
            } catch (JSONException e) {
                e.printStackTrace();
            }
            String sendData = jo.toString();
            try{
                URL url = new URL("http://www.o-ddang.com/theteacher/removePic.php");
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
            if(s.equals("RemoveSuccess")){
                Glide.with(rpContext).load(R.drawable.base_profile_img).into(ivProfilePic);
                SharedPreferences.Editor spEditor = sp.edit();
                spEditor.putString("picUrl", null);
                spEditor.commit();
            } else {
                // 사진 삭제 실패 시 확인 알람 창 띄워줍니다.
                alertDialogBuilder.setMessage("잠시 후 다시 시도해주세요.")
                        .setNeutralButton("OK", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        }).create().show();
            }
        }
    }

    // 유저가 요청한 비밀번호 수정을 서버에 요청하는 AsyncTask입니다.
    // 서버로 전송했을 때, 이번 비밀번호와 일치하지 않는 경우 ChangeProfileFail이 리턴됩니다.
    // ChangeProfile() 에서 불러집니다.
    public class ChangeProfile extends AsyncTask<String, String, String>{

        Context cpContext;

        // AsyncTask가 진행되는 동안 돌아갈 ProgressDialog입니다.
        ProgressDialog proDialog;
        // 비밀번호 변경에 실패하면 띄워줄 AlertDialog입니다.
        AlertDialog.Builder alertDialogBuilder;

        public ChangeProfile(Context context){
            cpContext = context;

            alertDialogBuilder = new AlertDialog.Builder(context);
            proDialog = new ProgressDialog(context);
            proDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            proDialog.show();
        }

        @Override
        protected String doInBackground(String... params) {
            String data = params[0];

            try{
                URL url = new URL("http://o-ddang.com/theteacher/changeProfile.php");
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

            if(result.equals("ChangeProfileSuccess")){
                Toast.makeText(cpContext, "비밀번호를 변경하였습니다.", Toast.LENGTH_SHORT).show();
            } else {
                // 비밀번호 변경 실패 시 확인 알람 창 띄워줍니다.
                alertDialogBuilder.setMessage("비밀번호를 확인해주세요.")
                        .setNeutralButton("OK", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        }).create().show();
            }
        }
    }




}
