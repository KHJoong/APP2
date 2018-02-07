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
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by kimhj on 2018-01-25.
 */

public class ProfileModifyActivity extends AppCompatActivity {

    private final static int CROP_CODE = 5555;
    private final static int CAMERA_CODE = 1111;
    private final static int GALLERY_CODE = 3333;

    private static String currentPhotoPath;
    private static Uri photoUri;

    Button btnCamera;
    Button btnGallery;
    Button btnPicRemove;
    ImageView ivProfilePic;
    TextView tvMyId;
    EditText etExPwd;
    EditText etNewPwd;
    EditText etNewPwdCheck;
    Button btnProfileChange;

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
        btnProfileChange = (Button) findViewById(R.id.btnProfileChange);

        if(!TextUtils.isEmpty(sp.getString("picUri", null))){
            Uri uri = Uri.parse(sp.getString("picUri", null));
            Glide.with(this).load(uri).into(ivProfilePic);
        }

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
                    break;
                case R.id.btnProfileChange:

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
//        Intent takePicGalleryIntent = new Intent(Intent.ACTION_GET_CONTENT);
        Intent takePicGalleryIntent = new Intent(Intent.ACTION_PICK);
//        takePicGalleryIntent.setType(MediaStore.Images.Media.CONTENT_TYPE);
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

    public void savePhoto(){
        File photo = new File(photoUri.getPath());
        UploadPic uploadPic = new UploadPic(this, currentPhotoPath);
        uploadPic.execute();
    }

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
            if(s.equals("UploadSuccess")){
                Glide.with(upContext).load(photoUri).into(ivProfilePic);
                SharedPreferences.Editor spEditor = sp.edit();
                spEditor.putString("picUri", photoUri.toString());
                spEditor.commit();

                Toast.makeText(upContext, "사진 파일 업데이트에 성공하였습니다.", Toast.LENGTH_SHORT).show();
            } else {
                // 로그인 실패 시 확인 알람 창 띄워줍니다.
                alertDialogBuilder.setMessage("사진 혹은 네트워크 상태를 다시 확인해주세요.")
                        .setNeutralButton("OK", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        }).create().show();
            }
        }
    }








}
