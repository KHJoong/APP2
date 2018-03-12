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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 다이얼로그 외부 화면 흐리게 표현
        WindowManager.LayoutParams lpWindow = new WindowManager.LayoutParams();
        lpWindow.flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND;
        lpWindow.dimAmount = 0.8f;
        getWindow().setAttributes(lpWindow);

        setContentView(R.layout.question_enroll_dialog);

        btnCamera = (Button)findViewById(R.id.btnCamera);
        btnGallery = (Button)findViewById(R.id.btnGallery);

        if(goCameraClickListener !=null && goGalleryClickListener !=null){
            btnCamera.setOnClickListener(goCameraClickListener);
            btnGallery.setOnClickListener(goGalleryClickListener);
        }
    }

    public QuestionEnroll_CustomDialog(Context con, View.OnClickListener goC, View.OnClickListener goG){
        super(con, android.R.style.Theme_Translucent_NoTitleBar);
        goCameraClickListener = goC;
        goGalleryClickListener = goG;
    }

}
