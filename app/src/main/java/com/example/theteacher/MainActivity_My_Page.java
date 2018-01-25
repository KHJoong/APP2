package com.example.theteacher;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Created by kimhj on 2018-01-25.
 */

public class MainActivity_My_Page extends Fragment {

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
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.main_my_page, container, false);

        btnCamera = (Button)view.findViewById(R.id.btnCamera);
        btnGallery = (Button)view.findViewById(R.id.btnGallery);
        btnPicRemove = (Button)view.findViewById(R.id.btnPicRemove);
        ivProfilePic = (ImageView)view.findViewById(R.id.ivProfilePic);
        tvMyId = (TextView)view.findViewById(R.id.tvMyId);
        etExPwd = (EditText)view.findViewById(R.id.etExPwd);
        etNewPwd = (EditText)view.findViewById(R.id.etNewPwd);
        etNewPwdCheck = (EditText)view.findViewById(R.id.etNewPwdCheck);
        btnProfileChange = (Button)view.findViewById(R.id.btnProfileChange);

        btnCamera.setOnClickListener(btnClickListener);
        btnGallery.setOnClickListener(btnClickListener);
        btnPicRemove.setOnClickListener(btnClickListener);
        btnProfileChange.setOnClickListener(btnClickListener);

        return view;
    }   // onCreateView 끝나는 부분입니다.


    Button.OnClickListener btnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()){
                case R.id.btnCamera:
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

}
