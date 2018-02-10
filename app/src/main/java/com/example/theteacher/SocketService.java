package com.example.theteacher;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;

/**
 * Created by kimhj on 2018-02-10.
 */

public class SocketService extends Service {

    String ip = "115.71.232.230"; // IP
    int port = 7777; // PORT번호

    // Netty server에 연결할 것이기 때문에 socket channel을 사용합니다.
    static SocketChannel socketChannel;

    // 서버로 보낼 데이터를 담을 JSONObject입니다.
    JSONObject jo;
    // 서버로 보낼 데이터에 ID를 담아 보내야 합니다.
    // 자신의 ID가 담겨있는 SharedPreferences를 선언합니다.
    SharedPreferences sp;
    String sendData;
    String otherId;

    // 서버에서부터 들어오는 신호를 받아주는 Thread입니다.
    // Signaling 서버이기 때문에 상담 전화 신호 요청을 받습니다.
    DataReceiver dataReceiver;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        dataReceiver = new DataReceiver();
        sp = getSharedPreferences("profile", MODE_PRIVATE);

        // 서버로 보낼 JSONObject입니다.
        // type의 join은 서버에 처음 연결하겠다는 요청 메시지입니다.
        jo = new JSONObject();
        try {
            jo.put("userId",sp.getString("id",""));
            jo.put("type", "join");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        sendData = jo.toString();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try{
                    socketChannel = SocketChannel.open();
                    socketChannel.configureBlocking(true);
                    socketChannel.connect(new InetSocketAddress(ip, port));

                    socketChannel.socket().getOutputStream().write(sendData.getBytes("EUC-KR"));
                }catch (Exception e){
                    e.printStackTrace();
                }
                // 서버에서 오는 Signaling을 받아주는 Thread가 시작되는 부분입니다.
                dataReceiver.start();
            }
        }).start();

    }

    class DataReceiver extends Thread{

        public void run(){
            while(true){
                ByteBuffer byteBuffer = ByteBuffer.allocate(256);
                try {
                    int readByteCount = socketChannel.read(byteBuffer);
                    Log.d("SocketService:", "DataReceiver:"+readByteCount+"");

                    if(readByteCount==-1){
                        throw new IOException();
                    }
                    byteBuffer.flip();
                    Charset charset = Charset.forName("EUC-KR");
                    JSONObject ob = new JSONObject(charset.decode(byteBuffer).toString());
                    otherId = ob.getString("userId");

                    Intent recSigIntent = new Intent(getApplicationContext(), ReceiveSignalActivity.class);
                    recSigIntent.putExtra("id", otherId);
                    startActivity(recSigIntent);

                } catch (IOException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }





}
