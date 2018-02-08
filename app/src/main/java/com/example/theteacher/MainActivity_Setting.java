package com.example.theteacher;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

/**
 * Created by kimhj on 2018-02-06.
 */

public class MainActivity_Setting extends Fragment{

    // 유저가 프로필을 수정할 수 있도록 배치한 버튼입니다.
    Button modifyMyProfile;
    // position 강사인 유저가 강의에 대한 정보를 관리할 수 있도록 배치한 버튼입니다.
    Button manageMyLecture;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.main_setting, container, false);

        SharedPreferences sp = getActivity().getSharedPreferences("profile", Context.MODE_PRIVATE);
        String position = sp.getString("position", "");

        modifyMyProfile = (Button)view.findViewById(R.id.modifyMyProfile);
        manageMyLecture = (Button)view.findViewById(R.id.manageMyLecture);

        modifyMyProfile.setOnClickListener(btnClickListener);
        manageMyLecture.setOnClickListener(btnClickListener);

        if(position.equals("teacher")){
            manageMyLecture.setVisibility(View.VISIBLE);
        } else {
            manageMyLecture.setVisibility(View.GONE);
        }

        return view;
    }

    Button.OnClickListener btnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()){
                case R.id.modifyMyProfile :
                    Intent goModiActivity = new Intent(getActivity().getApplicationContext(), ProfileModifyActivity.class);
                    startActivity(goModiActivity);
                    break;
                case R.id.manageMyLecture :
                    Intent goLectureActivity = new Intent(getActivity().getApplicationContext(), LectureManageActivity.class);
                    startActivity(goLectureActivity);
                    break;
            }
        }
    };
}
