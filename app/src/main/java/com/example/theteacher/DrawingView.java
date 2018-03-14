package com.example.theteacher;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.bumptech.glide.Glide;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.channels.NotYetConnectedException;
import java.nio.charset.Charset;
import java.util.concurrent.ExecutionException;
import java.util.logging.Handler;

/**
 * Created by kimhj on 2018-03-13.
 */

// 이 클래스는 QuestionViewActivity에서 사용하는 CustomView입니다.
// 다른 사람과 함께 그림 그릴 수 있게 하기 위해서 사용합니다.
// 사진을 찍어 질문을 올리고 그 사진 위에서 작업을 진행합니다.
public class DrawingView extends View{

    Context dvContext;

    private Canvas mCanvas;
    private Path mPath;
    private Paint mBitmapPaint;
    private Paint circlePaint;
    private Path circlePath;

    private float mX, mY;
    private float pX, pY;
    private float TOUCH_TOLERANCE = 4;

    int check;

    receiveThread rt;

    // 기본 생성자 3개입니다.
    public DrawingView(Context context) {
        super(context);
        dvContext = context;
        init(null);
    }

    public DrawingView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        dvContext = context;
        init(attrs);
    }

    public DrawingView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        dvContext = context;
        init(attrs);
    }

    // 초기화 부분입니다.
    // 이 부분은 자신이 클릭하는 부분에 동그란 원을 그려줘서 어디를 클릭하고 있는지 알 수 있도록 해주기위해 초기화하는 부분입니다.
    public void init(@Nullable AttributeSet attrs){
        mPath = new Path();
        mBitmapPaint = new Paint(Paint.DITHER_FLAG);
        circlePaint = new Paint();
        circlePath = new Path();
        circlePaint.setAntiAlias(true);
        circlePaint.setColor(Color.BLUE);
        circlePaint.setStyle(Paint.Style.STROKE);
        circlePaint.setStrokeJoin(Paint.Join.MITER);
        circlePaint.setStrokeWidth(4f);

        check = 0;

        rt = new receiveThread();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

//        mBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);

        // MainActivity_Question에서 생성한 bitmap을 바탕으로 canvas를 생성합니다.
        // 여기서 bitmap은 서버에 등록한 질문(사진)을 가지고 만들어진 bitmap입니다.
        mCanvas = new Canvas(MainActivity_Question.mBitmap);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // onSizeChanged에서 만든 canvas에 MainActivity_Question에서 생성한 bitmap을 그립니다.
        // 배경화면이 사진으로 바뀌는 부분입니다.
        canvas.drawBitmap( MainActivity_Question.mBitmap, 0, 0, mBitmapPaint);
        canvas.drawPath( mPath,  QuestionViewActivity.mPaint);
        canvas.drawPath( circlePath,  circlePaint);
    }

    public void receiveStart(){
        rt.start();
    }

    public void receiveStop(){
        if(!rt.isInterrupted()){
            rt.interrupt();
        }
        if(rt.isAlive()){
            rt.interrupt();
        }
    }

    // 상대방의 터치를 받아오는 부분입니다.
    // 클릭한 부분을 받아와서 내 화면에 그려줍니다.
    public class receiveThread extends Thread{
        @Override
        public void run() {
            super.run();
            String tmp = "";
            while (true){
                if(QuestionViewActivity.socketChannel==null){
                    break;
                }
                ByteBuffer byteBuffer = ByteBuffer.allocate(256);
                try {
                    int readByteCount = QuestionViewActivity.socketChannel.read(byteBuffer);
                    if (readByteCount == -1) {
                        throw new IOException();
                    }
                    byteBuffer.flip();
                    Charset charset = Charset.forName("EUC-KR");
                    String toOb = charset.decode(byteBuffer).toString();
                    Log.i("DrawingView:", "BufferRead:"+toOb);
                    tmp = tmp + toOb;
                    // 받아온 ByteBuffer를 String으로 변환시켜서 보면 {JSONObject}{JSONObject}{JSONO... 형식입니다.
                    // JSON을 한 덩어리씩 끊어내서 작업을 진행합니다.
                    String[] toObArr = tmp.split(System.getProperty("line.separator"));
                    for(int i=0; i<toObArr.length; i++){
                        // ByteBuffer의 크기 때문에 JSON이 완전한 덩어리로 받아지지 않는 경우가 있습니다.
                        // 따라서 String이 }를 가지고있는지 확인하여(JSON 형식이 완전히 끝났는지 확인) 작업을 진행합니다.
                        if(toObArr[i].contains("}")){
                            JSONObject ob = new JSONObject(toObArr[i]);
                            Log.i("DrawingView:", toObArr[i]);
                            float percX = Float.parseFloat(ob.getString("x"));
                            float percY = Float.parseFloat(ob.getString("y"));
                            final float x = MainActivity_Question.qWidth*percX/100;
                            final float y = MainActivity_Question.qHeight*percY/100;
                            if(ob.getString("set").equals("start")){
                                if(check == 0){
                                    mPath.moveTo(x, y);
                                    check = 1;
                                }
                            } else if(ob.getString("set").equals("move")){
                                if(check == 1){
                                    QuestionViewActivity.handler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            mPath.lineTo(x, y);
                                            invalidate();
                                        }
                                    });
                                }
                            } else if(ob.getString("set").equals("end")){
                                if(check == 1){
                                    QuestionViewActivity.handler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            mPath.lineTo(x, y);
                                            mCanvas.drawPath(mPath,  QuestionViewActivity.mPaint);
                                            invalidate();
                                            check = 0;
                                        }
                                    });
                                }
                            }
                            if(i==toObArr.length-1){
                                // {JSONObject} 딱 하나만 왔을 경우 사용한 후 tmp를 초기화하여 다음 추가할 때 영향을 끼치지 않도록 합니다.
                                tmp = "";
                            }
                        } else {
                            // String이 완전한 Json 형식이 아닌 경우 tmp에 저장하여 다음 메시지와 이어붙인 후 다시 작업을 진행하도록 합니다.
                            tmp = "";
                            tmp += toObArr[i];
                        }
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (NotYetConnectedException e){
                    e.printStackTrace();
                }
            }
        }
    }

    // 사용자가 화면을 클릭했을 때 시작하는 부분입니다.
    private void touch_start(float x, float y) {
        mPath.reset();
        mPath.moveTo(x, y);
        mX = x;
        mY = y;
        pX = mX/MainActivity_Question.qWidth*100;
        pY = mY/MainActivity_Question.qHeight*100;
        // 같은 방에 들어와있는 유저에게 찍은 점의 좌표를 전달해줍니다.
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    QuestionViewActivity.jo.put("type", "send_room");
                    // set에 start는 움직임이 시작했다는 것을 알리는 것입니다.
                    // moveTo를 사용할 수 있도록 전달해줍니다.
                    QuestionViewActivity.jo.put("set", "start");
                    QuestionViewActivity.jo.put("x", pX);
                    QuestionViewActivity.jo.put("y", pY);
                    ByteBuffer bf = ByteBuffer.wrap(QuestionViewActivity.jo.toString().getBytes("EUC-KR"));
                    QuestionViewActivity.socketChannel.write(bf);
                    bf.clear();
                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    // 사용자가 화면을 클릭한 상태로 이동할때 실행되는 부분입니다.
    private void touch_move(float x, float y) {
        float dx = Math.abs(x - mX);
        float dy = Math.abs(y - mY);
        if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
            mPath.quadTo(mX, mY, (x + mX)/2, (y + mY)/2);
            mX = x;
            mY = y;

            circlePath.reset();
            circlePath.addCircle(mX, mY, 30, Path.Direction.CW);
        }

        // 마찬가지로 같은 방에 들어와있는 유저에게 화면의 클릭한 부분을 전달합니다.
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    pX = mX/MainActivity_Question.qWidth*100;
                    pY = mY/MainActivity_Question.qHeight*100;

                    QuestionViewActivity.jo.put("type", "send_room");
                    // set의 move는 현재 클릭한채로 움직이고 있다는 것을 의미합니다.
                    // lineTo를 사용할 수 있도록 전달해줍니다.
                    QuestionViewActivity.jo.put("set", "move");
                    QuestionViewActivity.jo.put("x", pX);
                    QuestionViewActivity.jo.put("y", pY);
                    ByteBuffer bf = ByteBuffer.wrap(QuestionViewActivity.jo.toString().getBytes("EUC-KR"));
                    QuestionViewActivity.socketChannel.write(bf);
                    bf.clear();
                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    // 사용자가 화면에서 손을 뗼 때 실행되는 부분입니다.
    private void touch_up() {
        // 마찬가지로 같은 방에 들어와있는 유저에게 화면에서 손을 뗀 부분의 위치를 전달합니다.
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    QuestionViewActivity.jo.put("type", "send_room");
                    // set의 end는 화면에서 손을 뗀 것을 의미합니다.
                    QuestionViewActivity.jo.put("set", "end");
                    QuestionViewActivity.jo.put("x", pX);
                    QuestionViewActivity.jo.put("y", pY);
                    ByteBuffer bf = ByteBuffer.wrap(QuestionViewActivity.jo.toString().getBytes("EUC-KR"));
//                    MainActivity.socketChannel.socket().getOutputStream().write(MainActivity.jo.toString().getBytes("EUC-KR"));
                    QuestionViewActivity.socketChannel.write(bf);
                    bf.clear();
                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();


        mPath.lineTo(mX, mY);
        circlePath.reset();
        // commit the path to our offscreen
        mCanvas.drawPath(mPath,  QuestionViewActivity.mPaint);
        // kill this so we don't double draw
        mPath.reset();
    }

    // 사용자가 화면에 손을 데는 경우 모션을 받아들여 적절한 함수를 실행할 수 있도록 합니다.
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                touch_start(x, y);
                invalidate();
                break;
            case MotionEvent.ACTION_MOVE:
                touch_move(x, y);
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                touch_up();
                invalidate();
                break;
        }
        return true;
    }
}
