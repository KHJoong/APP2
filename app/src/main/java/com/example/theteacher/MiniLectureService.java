package com.example.theteacher;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.Toast;
import android.widget.VideoView;
import android.annotation.SuppressLint;

/**
 * Created by kimhj on 2018-04-05.
 */

// VideoView를 사용하여 setVideoUri, start를 하면 비디오 데이터를 받아옴
// 하지만 여기서 일정 시간만큼만 받아오고 대기하다가 다시 받아오는 기능이 없음
// 데이터를 완전히 받아온 후 VideoView가 재생하지 못한 파일이 5초 이상 남을 경우 ANR(Application Not Responding) 발생하는듯 함
public class MiniLectureService extends Service {

    // 동영상이 재생중일 때는 서비스가 유지되도록 START_CONTINUATION_MASK
    // 동영상이 끝난 경우 다시 서비스를 실행할 수 있도록 START_NOT_STICKY 를 담기 위한 변수입니다.
    int returnValue;
    // 강의에 대한 주소를 담고 있는 변수입니다.
    String lecUrl;
    // 동영상이 어디서부터 재생되면 되는지 담고있는 변수입니다.
    int current;

    // 화면에 영상을 띄우기 위해 사용되는 Inflater와 실제로 띄워질 뷰들입니다.
    LayoutInflater inflater;
    RelativeLayout viewContainer;
    VideoView vvMiniLecture;
    ImageButton btnExit;

    // 새로 띄워질 창의 크기, 외부 화면 터치 가능과 같은 설정을 하기 위해 사용됩니다.
    WindowManager.LayoutParams wmParams;
    // 화면을 통제하기 녀석입니다. 얘한테 위의 viewContainer를 추가하여 화면에 띄워달라 부탁합니다.
    // 영상 재생이 끝날 경우 화면에서 제거해달라고 부탁해야 합니다.
    WindowManager windowManager;

    // 띄워진 동영상 영상을 유저가 터치했을 때 몇 개의 손가락으로 터치했는지 구분하기 위한 변수입니다.
    // 한 손가락 터치, 두 손가락 터치 를 구분하여 mTouchMode에 담아 사용합니다.
    int mTouchMode = MODE_NONE;
    private static final int MODE_NONE = 0;
    private static final int MODE_ONE_TOUCH = 1;
    private static final int MODE_DOUBLE_TOUCH = 2;

    // 유저가 새롭게 터치를 시작한 부분의 좌표를 갖습니다.
    private float START_X;
    private float START_Y;
    // 기존에 띄워져 있는 영상의 좌표를 갖습니다.
    private int PREV_X;
    private int PREV_Y;
    // 두 손으로 터치했을 때 두 손가락 사이의 거리를 구하기 위해 사용됩니다.
    private float distance;
    boolean isSingleTap;

    Handler handler;

    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {
        handler = new Handler();

        new Thread(new Runnable() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        // RecordedViewActivity 에서 보던 강의의 주소를 담기 위한 변수입니다.
                        lecUrl = intent.getStringExtra("lecUrl");
                        // 강의를 어디서부터 재생하면 되는지 RecordedViewActivity 에서 전달해준 값을 담는 변수입니다.
                        current = intent.getIntExtra("current", 0);

                        // 창모드로 전환되었음을 알립니다.
                        Toast.makeText(MiniLectureService.this, "창모드로 전환합니다.", Toast.LENGTH_SHORT).show();

                        // 창모드로 띄워주는 뷰들입니다.
                        inflater = LayoutInflater.from(MiniLectureService.this);
                        viewContainer = (RelativeLayout) inflater.inflate(R.layout.miniview_lecture, null);
                        vvMiniLecture = (VideoView)viewContainer.findViewById(R.id.vvMiniLecture);
                        btnExit = (ImageButton)viewContainer.findViewById(R.id.btnExit);

                        // 처음에는 종료 버튼을 보이지 않게 합니다.
                        // 한 번의 터치를 인식했을 때 보여줍니다.
                        btnExit.setVisibility(View.GONE);
                        // 버튼을 클릭하면 창모드를 종료하도록 합니다.
                        btnExit.setOnClickListener(btnClickListener);
                        // 재생이 완료되면 취할 동작을 등록합니다. 여기서는 재생이 완료되면 종료되도록 설정했습니다.
                        vvMiniLecture.setOnCompletionListener(vvCompleteListener);
                        // 영상을 터치했을 때 모션을 등록합니다.
                        vvMiniLecture.setOnTouchListener(viewTouchListener);

                        // WindowManager가 실질적으로 띄워주는 역할을 합니다.
                        // 화면의 총 감독관 같은 역할입니다.
                        // 띄울 땐 addView를 사용하고 제거할 땐 removeView를 사용합니다.
                        windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);

                        wmParams = new WindowManager.LayoutParams(
                                WindowManager.LayoutParams.WRAP_CONTENT,
                                WindowManager.LayoutParams.WRAP_CONTENT,
                                WindowManager.LayoutParams.TYPE_PHONE,              // 영상이 다른 App 화면보다 제일 위에 뜨도록
                                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,      // 영상 외 부분을 터치할 수 있도록
                                PixelFormat.TRANSLUCENT
                        );
                        wmParams.gravity = Gravity.CENTER_HORIZONTAL | Gravity.TOP; // 처음 영상을 생성하는 위치
                        wmParams.x = 0;
                        wmParams.y = 0;

                        // VideoView에 재생할 영상의 주소를 등록합니다.
                        vvMiniLecture.setVideoURI(Uri.parse(lecUrl));
                        // 재생하면 되는 부분의 시간을 설정합니다.
                        vvMiniLecture.seekTo(current);
                        vvMiniLecture.start();

                        // WindowManager에 띄울 뷰와 미리 설정한 설정값을 함께 등록합니다.
                        windowManager.addView(viewContainer, wmParams);
                    }
                });
            }
        }).start();

        // 재생 중일 경우 START_CONTINUATION_MASK
        // 재생 중이 아닐 경우 START_NOT_STICKY 를 리턴합니다.
        return returnValue;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // 영상이 이제 시작되고자 하는 부분이기 때문에 START_CONTINUATION_MASK를 리턴할 수 있도록 합니다.
        // START_CONTINUATION_MASK을 리턴해야 App이 죽어도 서비스가 재생하던 부분부터 이어서 되살아납니다.
        returnValue = START_CONTINUATION_MASK;
        handler = new Handler();
    }

    @Override
    public void onDestroy() {
        // 서비스가 종료되면 WindowManager에 등록했던 뷰를 제거하고
        // 뷰와 Windowmanager를 초기화합니다.
        if(windowManager!=null){
            if(viewContainer!=null) {
                windowManager.removeView(viewContainer);
                viewContainer = null;
            }
            windowManager = null;
        }
        // 서비스를 완전히 종료합니다.
        stopSelf();
        super.onDestroy();
    }

    // 영상이 종료되었을 때의 액션입니다.
    private MediaPlayer.OnCompletionListener vvCompleteListener = new MediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(MediaPlayer mp) {
            // 영상 재생이 완료되면 다른 서비스를 또 시작할 수 있도록 return 값을 변경합니다.
            returnValue = START_NOT_STICKY;
            stopSelf();

        }
    };

    // 버튼을 클릭했을 때의 액션입니다.
    private View.OnClickListener btnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()){
                // 영상 종료 버튼을 클릭했을 때
                case R.id.btnExit:
                    // 영상을 종료하면 다른 서비스를 또 시작할 수 있도록 return 값을 변경합니다.
                    returnValue = START_NOT_STICKY;
                    stopSelf();
                    break;
            }
        }
    };

    // 두 손가락으로 터치했을 때 두 손가락 사이의 거리를 구하는 함수입니다.
    @SuppressLint("FloatMath")
    private float doubleTouchDistance(MotionEvent event) {
        // 두 손가락이 터치한 부분의 x 좌표의 차이를 담습니다.
        float x = event.getX(0) - event.getX(1);
        // 두 손가락이 터치한 부분의 y 좌표의 차이를 담습니다.
        float y = event.getY(0) - event.getY(1);
        // 거리를 구합니다.
        distance = (float) (Math.sqrt(x * x + y * y));

        return distance;
    }

    // 영상을 터치했을 때의 액션입니다.
    private View.OnTouchListener viewTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            try{
                switch (event.getAction() & MotionEvent.ACTION_MASK) {
                    // 한 손가락으로 터치했을 때입니다.
                    case MotionEvent.ACTION_DOWN:
                        isSingleTap = true;

                        mTouchMode = MODE_ONE_TOUCH;
                        btnExit.setVisibility(View.INVISIBLE);

                        START_X = event.getRawX();
                        START_Y = event.getRawY();
                        PREV_X = wmParams.x;
                        PREV_Y = wmParams.y;

                        break;

                    // 한 손가락을 터치하면서 움직일 때입니다.
                    case MotionEvent.ACTION_MOVE:
                        if(mTouchMode == MODE_ONE_TOUCH){
                            int x = (int) (event.getRawX() - START_X);
                            int y = (int) (event.getRawY() - START_Y);
                            wmParams.x = PREV_X + x;
                            wmParams.y = PREV_Y + y;
                            windowManager.updateViewLayout(viewContainer, wmParams);
                        }
                        if(mTouchMode == MODE_DOUBLE_TOUCH){
                            isSingleTap = false;
                            float distance = doubleTouchDistance(event);
                            wmParams.width = (int) distance;
                            windowManager.updateViewLayout(viewContainer, wmParams);
                        }
                        break;

                    // 한 손가락을 떼었을 때입니다.
                    case MotionEvent.ACTION_UP:
                        btnExit.setVisibility(View.VISIBLE);
                        windowManager.updateViewLayout(viewContainer, wmParams);
                        // 영상을 클릭해서 보여줬던 버튼들을 다시 숨기는 부분입니다.
                        // 3초 후에 숨기도록 설정해둡니다.
                        btnExit.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                btnExit.setVisibility(View.GONE);
                            }
                        }, 3000);
                        break;

                    // 두 손가락으로 터치했을 때입니다.
                    case MotionEvent.ACTION_POINTER_DOWN:
                        mTouchMode = MODE_DOUBLE_TOUCH;
                        isSingleTap = false;
                        break;

                    // 두 손가락을 떼었을 때입니다.
                    case MotionEvent.ACTION_POINTER_UP:
                        mTouchMode = MODE_NONE;
                        isSingleTap = false;
                        break;
                }
            } catch (NullPointerException e){
                // 재생이 끝나서 화면이 종료될 때, 유저가 여전히 크기를 조절하고 있을 경우 NullPointerException 발생
                // 그 상황에 대한 예외 처리
                Toast.makeText(MiniLectureService.this, "재생이 종료되었습니다.", Toast.LENGTH_SHORT).show();
            }
            return true;
        }
    };

}
