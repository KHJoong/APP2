package com.example.theteacher;

import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;

/**
 * Created by kimhj on 2018-01-24.
 * 이 클래스는 TheTeacher에서 쓰인 SharedPerferences를 정리하여 보관하기 위해 만들어두었습니다.
 */

public class _ref extends AppCompatActivity{


    // isLogged, id, position, sessionID가 저장되어 있는 sharedPreferences입니다.
    // LoginActivity에서 로그인이 성공하면 저장합니다.
    // SplashActivity에서 isLogged 값을 확인한 후 로그인 된 상태면 MainActivity로, 안된 상태면 LoginActivity로 화면을 엽니다.
    SharedPreferences sharedPreferences = getSharedPreferences("profile", MODE_PRIVATE);
}
