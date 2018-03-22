package com.example.theteacher;

import android.annotation.TargetApi;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import static android.app.Activity.RESULT_OK;

/**
 * Created by kimhj on 2018-03-22.
 */

public class MainActivity_Recorded extends Fragment {

    FloatingActionButton fabEnroll;

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

        fabEnroll = (FloatingActionButton)view.findViewById(R.id.fabEnroll);
        fabEnroll.setOnClickListener(btnClickListener);


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

                // 등록할 강의의 제목을 입력받기 위해 custom dialog를 띄워줍니다.
                RecordedEnroll_CustomDialog reDialog = new RecordedEnroll_CustomDialog(getActivity());
                reDialog.setCancelable(true);
                reDialog.getWindow().setGravity(Gravity.CENTER);
                reDialog.show();
            }
        }
    }






}
