package com.example.theteacher;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class LoginActivity extends AppCompatActivity {

    Button btnLogin;
    Button btnMemberJoin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        btnLogin = (Button)findViewById(R.id.btnLogin);
        btnMemberJoin = (Button)findViewById(R.id.btnMemberJoin);

        btnLogin.setOnClickListener(btnClickListener);
        btnMemberJoin.setOnClickListener(btnClickListener);

    } // onCreate 끝나는 부분입니다.

    Button.OnClickListener btnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()){
                case R.id.btnLogin:
                    break;
                case R.id.btnMemberJoin:
                    Intent intent = new Intent(getApplicationContext(), MemberJoinActivity.class);
                    startActivity(intent);
                    break;
            }
        }
    };


}
