package com.example.theteacher;

import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;

/**
 * Created by kimhj on 2018-01-24.
 */

public class _ref extends AppCompatActivity{


    // id, position, sessionID가 저장되어 있는 sharedPreferences입니다.
    SharedPreferences sharedPreferences = getSharedPreferences("profile", MODE_PRIVATE);
}
