package com.example.theteacher;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

/**
 * Created by kimhj on 2018-03-13.
 */

public class QuestionEnroll_CustomDialog extends Dialog {

    Button btnCamera;
    Button btnGallery;

    View.OnClickListener goCameraClickListener;
    View.OnClickListener goGalleryClickListener;

    // 생성자에서 변수로 받은 Listener를 위의 버튼에 등록할 수 있도록 변수에 담아두는 부분입니다.
    public QuestionEnroll_CustomDialog(Context con, View.OnClickListener goC, View.OnClickListener goG){
        super(con, android.R.style.Theme_Translucent_NoTitleBar);
        goCameraClickListener = goC;
        goGalleryClickListener = goG;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 다이얼로그 외부 화면 흐리게 표현
        WindowManager.LayoutParams lpWindow = new WindowManager.LayoutParams();
        lpWindow.flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND;
        lpWindow.dimAmount = 0.8f;
        getWindow().setAttributes(lpWindow);

        setContentView(R.layout.question_enroll_dialog);

        // 카메라로 사진을 찍게할지
        // 갤러리에서 사진을 가져올지
        // 두 경우를 선택할 수 있도록 버튼을 두개 띄웁니다.
        btnCamera = (Button)findViewById(R.id.btnCamera);
        btnGallery = (Button)findViewById(R.id.btnGallery);

        if(goCameraClickListener !=null && goGalleryClickListener !=null){
            // 생성자에서 담아둔 listener를 버튼에 등록합니다.
            btnCamera.setOnClickListener(goCameraClickListener);
            btnGallery.setOnClickListener(goGalleryClickListener);
        }
    }

}
