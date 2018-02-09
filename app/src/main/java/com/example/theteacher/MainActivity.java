package com.example.theteacher;

import android.annotation.SuppressLint;
import android.app.Fragment;
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

    // 슬라이딩 메뉴 버튼을 클릭 시 다음 메뉴가 나타나게 됩니다.
    // 위의 menuList 배열에 다음 메뉴 이름을 String으로 담습니다.
    public MainActivity() {
        Collections.addAll(menuList, new String[]{
                "Home",
                "Setting"
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        dlSlidingMenu = (DrawerLayout)findViewById(R.id.dlSlidingMenu);
        lvMenuList = (ListView)findViewById(R.id.lvMenuList);

        setActionBar();
        setSlidingMenu();
        setDrawer();

        if(savedInstanceState == null){
            displayView(0);
            getSupportActionBar().setTitle(R.string.app_name);
        }

    } // onCreate 끝부분입니다.

    @ Override
    public void onBackPressed() {
        if (dlSlidingMenu.isDrawerOpen(lvMenuList)) {
            dlSlidingMenu.closeDrawer(lvMenuList);
        } else {
            super.onBackPressed();
        }
    }

    private void displayView(int position) {
        Fragment fragment = null;
        switch (position) {
            case 0:
                fragment = new MainActivity_Home();
                break;
            case 1:
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
        // 토글이 이벤트를 소비했다면 이벤트를 전파시키지 않고 종결한다.
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
