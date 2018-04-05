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
import android.util.FloatMath;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.MediaController;
import android.widget.RelativeLayout;
import android.widget.Toast;
import android.widget.VideoView;
import android.annotation.SuppressLint;

/**
 * Created by kimhj on 2018-04-05.
 */

public class MiniLectureService extends Service {

    int returnValue;
    String lecUrl;
    int current;

    LayoutInflater inflater;
    RelativeLayout viewContainer;
    VideoView vvMiniLecture;
    ImageButton btnExit;

    WindowManager.LayoutParams wmParams;
    WindowManager windowManager;

    Handler handler;


    private static final int MODE_NONE = 0;
    private static final int MODE_ONE_TOUCH = 1;
    private static final int MODE_DOUBLE_TOUCH = 2;

    boolean isSingleTap;
    int mTouchMode = MODE_NONE;
    private float START_X;
    private float START_Y;
    private float distance;
    private int PREV_X;
    private int PREV_Y;


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        handler = new Handler();
        lecUrl = intent.getStringExtra("lecUrl");
        current = intent.getIntExtra("current", 0);

//        returnValue = START_NOT_STICKY;
//        super.onStartCommand(intent, flags, startId)
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

        returnValue = START_CONTINUATION_MASK;
        handler = new Handler();

        new Thread(new Runnable() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MiniLectureService.this, "창모드로 전환합니다.", Toast.LENGTH_SHORT).show();

                        inflater = LayoutInflater.from(MiniLectureService.this);
                        viewContainer = (RelativeLayout) inflater.inflate(R.layout.miniview_lecture, null);
                        vvMiniLecture = (VideoView)viewContainer.findViewById(R.id.vvMiniLecture);
                        btnExit = (ImageButton)viewContainer.findViewById(R.id.btnExit);

                        windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);

                        vvMiniLecture.setOnTouchListener(viewTouchListener);
                        btnExit.setVisibility(View.GONE);
                        btnExit.setOnClickListener(btnClickListener);

                        wmParams = new WindowManager.LayoutParams(
                                WindowManager.LayoutParams.WRAP_CONTENT,
                                WindowManager.LayoutParams.WRAP_CONTENT,
                                WindowManager.LayoutParams.TYPE_PHONE,
                                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                                PixelFormat.TRANSLUCENT
                        );
                        wmParams.gravity = Gravity.CENTER_HORIZONTAL | Gravity.TOP;
                        wmParams.x = 0;
                        wmParams.y = 0;

                        // VideoView 영상 실행
                        vvMiniLecture.setVideoURI(Uri.parse(lecUrl));
                        vvMiniLecture.seekTo(current);
                        vvMiniLecture.setOnCompletionListener(vvCompleteListener);
                        vvMiniLecture.start();

                        windowManager.addView(viewContainer, wmParams);
                    }
                });
            }
        }).start();
    }

    @Override
    public void onDestroy() {
        if(windowManager!=null){
            if(viewContainer!=null) {
                windowManager.removeView(viewContainer);
                viewContainer = null;
            }
            windowManager = null;
        }
        stopSelf();
        super.onDestroy();
    }

    @SuppressLint("FloatMath")
    private float doubleTouchDistance(MotionEvent event) {

        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        distance = (float) (Math.sqrt(x * x + y * y));

        //화면 밖으로 나가지 못하도록 Block
//        if(distance >= mWidth){
//            distance = mWidth ;
//            return distance;
//        }

        return distance;
    }

    private MediaPlayer.OnCompletionListener vvCompleteListener = new MediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(MediaPlayer mp) {

            returnValue = START_NOT_STICKY;

        }
    };

    private View.OnClickListener btnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if(windowManager!=null){
                if(viewContainer!=null) {
                    windowManager.removeView(viewContainer);
                    viewContainer = null;
                }
                windowManager = null;
            }
        }
    };


    private View.OnTouchListener viewTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {

            switch (event.getAction() & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_DOWN:

                    isSingleTap = true;
                    mTouchMode = MODE_ONE_TOUCH;
                    btnExit.setVisibility(View.INVISIBLE);

                    START_X = event.getRawX();
                    START_Y = event.getRawY();
                    PREV_X = wmParams.x;
                    PREV_Y = wmParams.y;

                    break;

                case MotionEvent.ACTION_MOVE:    // 이동 시

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

                case MotionEvent.ACTION_UP:

                    btnExit.setVisibility(View.VISIBLE);
                    windowManager.updateViewLayout(viewContainer, wmParams);
                    btnExit.setVisibility(View.VISIBLE);
                    break;

                case MotionEvent.ACTION_POINTER_DOWN:    // 두 손가락 터치 시
                    mTouchMode = MODE_DOUBLE_TOUCH;

                    isSingleTap = false;

                    break;

                case MotionEvent.ACTION_POINTER_UP:        // 두 손가락을 떼었을 시

                    isSingleTap = false;
                    mTouchMode = MODE_NONE;
                    break;
            }
            return true;
        }
    };


}
