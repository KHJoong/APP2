package com.example.theteacher;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.profile_modify);

        btnCamera = (Button) findViewById(R.id.btnCamera);
        btnGallery = (Button) findViewById(R.id.btnGallery);
        btnPicRemove = (Button) findViewById(R.id.btnPicRemove);
        ivProfilePic = (ImageView) findViewById(R.id.ivProfilePic);
        tvMyId = (TextView) findViewById(R.id.tvMyId);
        etExPwd = (EditText) findViewById(R.id.etExPwd);
        etNewPwd = (EditText) findViewById(R.id.etNewPwd);
        etNewPwdCheck = (EditText) findViewById(R.id.etNewPwdCheck);
        btnProfileChange = (Button) findViewById(R.id.btnProfileChange);

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
                    TakePicIntent();
                    break;
                case R.id.btnGallery:
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

    public void TakePicIntent(){
        Intent takePicIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        if(takePicIntent.resolveActivity(getPackageManager()) != null){
            File photoFile = null;
            try {
                // 사진 찍은 시간을 기준으로 yyyyMMdd_HHmmss.jpg 파일을 만듭니다.
                // photoFile은 그 만들어진 파일의 절대경로를 담고 있는 변수입니다.
                photoFile = CreateImageFile();
            } catch (IOException e){
                Log.i("ProfileModifyAct:","TakePickIntent:"+e.toString());
            }
            if(photoFile != null){
                // photoURI : file://로 시작, FileProvider(Content Provider 하위)는 content://로 시작
                // 누가(7.0)이상부터는 file://로 시작되는 Uri의 값을 다른 앱과 주고 받기가 불가능하여 content://로 변경
                Uri providerURI = FileProvider.getUriForFile(getApplicationContext(), "com.example.theteacher", photoFile);
                photoUri = providerURI;
                Log.i("imageUri", photoUri.toString());

                // 인텐트에 전달할 때는 FileProvier의 Return값인 content://로만!!, providerURI의 값에 카메라 데이터를 넣어 보냄
                takePicIntent.putExtra(MediaStore.EXTRA_OUTPUT, providerURI);

                // file provider permission denial 해결 부분, 패키지를 필요로 하는 모든 패키지에 권한 부여 해줌
                List<ResolveInfo> resInfoList = getApplicationContext().getPackageManager().queryIntentActivities(takePicIntent, PackageManager.MATCH_DEFAULT_ONLY);
                for (ResolveInfo resolveInfo : resInfoList) {
                    String packageName = resolveInfo.activityInfo.packageName;
                    getApplicationContext().grantUriPermission(packageName, photoUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                }

                startActivityForResult(takePicIntent, CAMERA_CODE);
            }
        }
    }

    // 카메라로 찍은 사진의 이름을 지정해주는 함수입니다.
    // 사진을 찍은 시간으로 jpg 파일을 만듭니다.
    public File CreateImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = timeStamp + ".jpg";
        File imageFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/TheTeacher/" + imageFileName);
        Log.i("ProfileModifyAct:", "createImageFile:"+imageFileName);

        // 만약 위의 imageFile 경로에 폴더가 존재하지 않는다면 새로 만들어주는 부분입니다.
        if(!imageFile.exists()) {
            imageFile.getParentFile().mkdirs();
            imageFile.createNewFile();
        }

        currentPhotoPath = imageFile.getAbsolutePath();

        return imageFile;
    }

    public void CropPic(Uri uri){
        Intent cropPictureIntent = new Intent("com.android.camera.action.CROP");
        cropPictureIntent.setDataAndType(uri, "image/*");
        cropPictureIntent.putExtra("output", uri);
        cropPictureIntent.putExtra("outputX", 640); // crop한 이미지의 x축 크기 (integer)
        cropPictureIntent.putExtra("outputY", 480); // crop한 이미지의 y축 크기 (integer)
        cropPictureIntent.putExtra("aspectX", 4); // crop 박스의 x축 비율 (integer)
        cropPictureIntent.putExtra("aspectY", 3); // crop 박스의 y축 비율 (integer)
        cropPictureIntent.putExtra("scale", true);
        cropPictureIntent.putExtra("return-data", true);
        if (cropPictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(cropPictureIntent, CROP_CODE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode==RESULT_OK){
            if(requestCode==CAMERA_CODE){
                Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                File file = new File(currentPhotoPath);
                photoUri = Uri.fromFile(file);
                mediaScanIntent.setData(photoUri);
                sendBroadcast(mediaScanIntent);

                CropPic(photoUri);
            } else if(resultCode==CROP_CODE){
                File file = new File(currentPhotoPath);

            }
        }
    }










}
