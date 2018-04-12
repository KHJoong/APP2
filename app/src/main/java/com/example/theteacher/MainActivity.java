package com.example.theteacher;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Fragment;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.PersistableBundle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.gun0912.tedpermission.PermissionListener;
import com.gun0912.tedpermission.TedPermission;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by kimhj on 2018-01-24.
 */

public class MainActivity extends AppCompatActivity {

    private DrawerLayout dlSlidingMenu;
    private ListView lvMenuList;

    // 액티비티에 ActionBar가 포함되어 있으므로
    // DrawerListener 구현 대신 ActionBarDrawerToggle 클래스를 사용합니다.
    private ActionBarDrawerToggle drawerToggle;
    // 슬라이딩 메뉴에 어떤 목록이 있는지 String 값을 담을 배열입니다..
    private ArrayList< String > menuList = new ArrayList< String >();
    // action bar의 타이틀을 바꾸기 위해 두 변수를 사용합니다.
    // baseName은 앱의 이름을 담고있습니다.
    // menuName은 클릭한 메뉴의 이름을 담고 있습니다.
    private CharSequence baseName;
    private CharSequence menuName;
    // 프래그먼트 전환을 부드럽게 처리하기 위하여 스레드를 사용해 처리합니다.
    // 그 때 사용하기 위한 Handler입니다.
    private Handler hdrSwitchFrag = new Handler();
    // user의 타입이 teacher인지 student인지 담고 있는 shared입니다.
    private SharedPreferences sp;

    // 슬라이딩 메뉴 버튼을 클릭 시 다음 메뉴가 나타나게 됩니다.
    // 위의 menuList 배열에 다음 메뉴 이름을 String으로 담습니다.
    public MainActivity() {
        Collections.addAll(menuList, new String[]{
                "Home",
                "NowPlaying",
                "Recorded",
                "Question",
                "Setting"
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        // Signaling 서버와의 소켓 연결을 위한 SocketService 실행 --------------------------------------
        Intent socketIntent = new Intent(getApplicationContext(), SocketService.class);
        startService(socketIntent);
        // -----------------------------------------------------------------------------------------

        dlSlidingMenu = (DrawerLayout)findViewById(R.id.dlSlidingMenu);
        lvMenuList = (ListView)findViewById(R.id.lvMenuList);

        setActionBar();
        setSlidingMenu();
        setDrawer();

        if(savedInstanceState == null){
            displayView(0);
            getSupportActionBar().setTitle(R.string.app_name);
        }

        // TedPermission Library 사용 부분입니다.
        // 강사로 로그인 했을 경우 언제든 영상 통화를 받을 수 있어야 하기에 카메라, 오디오 권한을 받습니다.
        // 하지만 학생의 경우 영상 상담 요청을 하지 않을 경우 필요하지 않기에 여기서 요구하지 않습니다.
        sp = getSharedPreferences("profile", MODE_PRIVATE);
        if(sp.getString("position", "").equals("teacher")){
            TedPermission.with(this)
                    .setPermissionListener(permissionlistener)
                    .setDeniedMessage("영상 상담을 받기 위해 필요한 권한입니다.\n권한을 허가하셔야 상담을 받을 수 있습니다.\n[Setting] > [Permission]")
                    .setPermissions(Manifest.permission.INTERNET, Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO, Manifest.permission.MODIFY_AUDIO_SETTINGS)
                    .check();
        } else {
            TedPermission.with(this)
                    .setPermissionListener(permissionlistener)
                    .setDeniedMessage("TheTeacher은 인터넷 권한이 있어야 사용할 수 있습니다. \n[Setting] > [Permission]에서 설정해주시기 바랍니다.")
                    .setPermissions(Manifest.permission.INTERNET)
                    .check();
        }

    } // onCreate 끝부분입니다.

    // TedPermission Library 사용 부분입니다.
    // 허락했을 때, 거부했을 때의 Action으로 구성합니다.
    PermissionListener permissionlistener = new PermissionListener() {
        @Override
        public void onPermissionGranted() {
            Toast.makeText(MainActivity.this, "감사합니다.", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onPermissionDenied(ArrayList<String> deniedPermissions) {
            if(sp.getString("position", "").equals("teacher")){
                Toast.makeText(MainActivity.this, "권한이 거부되었습니다.\n 영상 상담 전화를 받을 수 없습니다.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MainActivity.this, "권한이 거부되었습니다.\n", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    };

    @ Override
    public void onBackPressed() {
        if (dlSlidingMenu.isDrawerOpen(lvMenuList)) {
            dlSlidingMenu.closeDrawer(lvMenuList);
        } else {
            super.onBackPressed();
        }
    }

    // 메뉴를 클릭했을 경우 어떤 화면을 띄울지 메뉴 리스트의 position에 따라 결정됩니다.
    private void displayView(int position) {
        Fragment fragment = null;
        switch (position) {
            case 0:
                // 홈 화면을 띄워주는 Fragment입니다.
                fragment = new MainActivity_Home();
                break;
            case 1:
                // 현재 진행되고 있는 강의 목록을 보여주는 Fragment입니다.
                fragment = new MainActivity_NowPlaying();
                break;
            case 2:
                fragment = new MainActivity_Recorded();
                break;
            case 3:
                // 질문 글 목록을 보여주는 Fragment입니다.
                fragment = new MainActivity_Question();
                break;
            case 4:
                // 설정 화면을 띄워주는 Fragment입니다.
                fragment = new MainActivity_Setting();
                break;
            default:
                break;
        }

        if (fragment != null) {
            // 프래그먼트 전환의 부드러운 처리를 위해 스레드로 처리합니다.
            hdrSwitchFrag.post(new CommitFragmentRunnable(fragment));

            lvMenuList.setItemChecked(position, true);
            lvMenuList.setSelection(position);
            dlSlidingMenu.closeDrawer(lvMenuList);

        } else {
            Log.i("MainAct:displayView:", "error in displayView(int position)");
        }
    }

    // 왼쪽 위, 슬라이딩 메뉴를 열거나 닫을 수 있는 버튼을 만들어주는 기능을 합니다.
    private void setActionBar() {
        menuName = baseName = getTitle();

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

    }

    // 슬라이딩 메뉴 안의 메뉴 이름과 listview를 연결해주는 설정
    // 그 메뉴를 클릭했을 때의 액션을 정의하는 함수입니다.
    private void setSlidingMenu() {
        // MainActivity()에서 등록한 메뉴 이름을 연결해줄 Adapter입니다.
        ArrayAdapter<String> adapter = new ArrayAdapter < String > (this, android.R.layout.simple_list_item_activated_1);
        // MainActivity()에서 등록한 메뉴 이름을 Adapter와 연결해주는 부분입니다.
        adapter.addAll(menuList);

        lvMenuList.setAdapter(adapter);
        lvMenuList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @ Override
            public void onItemClick(AdapterView <  ?  > parent, View view, int position, long id) {
                menuName = menuList.get(position);
                if(position==0){
                    getSupportActionBar().setTitle(baseName);
                } else {
                    getSupportActionBar().setTitle(menuName);
                }
                displayView(position);
            }
        });
    }

    // 왼쪽 위, 슬라이딩 메뉴 버튼을 클릭했을 때의 액션을 정의하는 함수입니다.
    // 액션 바의 이름의 기본값은 Home입니다.
    // 메뉴를 클릭했을 때 그 메뉴의 이름으로 액션 바의 이름을 바꿔줍니다.
    private void setDrawer() {
        drawerToggle = new ActionBarDrawerToggle(this, dlSlidingMenu, R.string.app_name, R.string.app_name) {
            @ Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                if(menuName.equals("Home")){
                    getSupportActionBar().setTitle(baseName);
                } else {
                    getSupportActionBar().setTitle(menuName);
                }
            }

            @ Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                if(menuName.equals("Home")){
                    getSupportActionBar().setTitle(baseName);
                } else {
                    getSupportActionBar().setTitle(menuName);
                }
            }
        };

        drawerToggle.setDrawerIndicatorEnabled(true);

        dlSlidingMenu.setDrawerListener(drawerToggle);
        dlSlidingMenu.setDrawerShadow(android.R.drawable.menu_frame, GravityCompat.START);
    }

    @ Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // 토글이 이벤트를 소비했다면 이벤트를 전파시키지 않고 종결합니다.
        // 이 부분이 있어야 메뉴를 클릭했을 때 정상적으로 작동합니다.
        if (drawerToggle.onOptionsItemSelected(item)) {
            return true;
        } else {
            return false;
        }
    }

    // 액션바 토글 상태를 동기화하기 위해서 다음 두 개의 메서드를 오버라이드 한다.
    @SuppressLint("NewApi")
    public void onPostCreate(Bundle savedInstanceState, PersistableBundle persistentState) {
        super.onPostCreate(savedInstanceState, persistentState);
        drawerToggle.syncState();
    }

    @ Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        drawerToggle.onConfigurationChanged(newConfig);
    }

    private class CommitFragmentRunnable implements Runnable {
        private Fragment fragment;

        public CommitFragmentRunnable(Fragment fragment) {
            this.fragment = fragment;
        }

        @ Override
        public void run() {
            getFragmentManager().beginTransaction().replace(R.id.flMenu, fragment).commit();
        }
    }


}
