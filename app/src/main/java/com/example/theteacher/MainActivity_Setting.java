package com.example.theteacher;

import android.app.Fragment;
import android.content.Intent;
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

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.main_setting, container, false);

        modifyMyProfile = (Button)view.findViewById(R.id.modifyMyProfile);
        modifyMyProfile.setOnClickListener(btnClickListener);

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
            }
        }
    };
}
