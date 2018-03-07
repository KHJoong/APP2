package com.example.theteacher;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.util.Log;
import android.widget.AbsListView;
import android.widget.ListView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;

/**
 * Created by kimhj on 2018-03-06.
 */


public class LecChatThread extends Thread{

    Context lcContext;
    ListView lclv;
    LecChat_Adapter lcAdapter;
    Handler handler;

    // 서버로 전송될 데이터를 json object 형태로 묶어줄 때 사용합니다.
    JSONObject jo;

    // 연결될 채팅 서버의 ip와 그때 사용되는 port 번호입니다.
    String ip = "115.71.232.230";
    int port = 8888;
    SocketChannel socketChannel;

    // 현재 유저의 id를 담을 변수입니다.
    String id;
    // 현재 유저가 접속해있는 방의 이름을 담는 변수입니다.
    String rId;

    // 메시지 리스트뷰에 아이템이 추가되었을 때, 어디를 보여주고 있어야 할 지 정하기 위해 사용합니다.
    // 중간을 보고있었다면 메시지가 추가되도 그 위치를 유지하고
    // 맨 아래를 보고 있었다면 메시지가 추가되도 맨 아래를 볼 수 있도록 합니다.
    int firVisibleNum, visibleCount, totalCount;

    // 페이징 값입니다. 초기 값은 10으로 설정되어 있습니다.(0부터 10개 불러오란 의미)
    // 메시지 추가 요청 시 10을 더해줍니다.(다음 10개를 가져올 수 있도록)
    int paging;

    LecChatThread(Context c, ListView lv, LecChat_Adapter a, String r) {
        handler = new Handler();
        jo = new JSONObject();

        lcContext = c;
        lclv = lv;
        lcAdapter = a;
        rId = r;

        SharedPreferences sp = lcContext.getSharedPreferences("profile", Context.MODE_PRIVATE);
        id = sp.getString("id", "");

        paging = 10;
    }

    @Override
    public void run() {
        try {
            lclv.setOnScrollListener(new AbsListView.OnScrollListener() {
                @Override
                public void onScrollStateChanged(AbsListView view, int scrollState) {

                }

                @Override
                public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                    firVisibleNum = firstVisibleItem;
                    visibleCount = visibleItemCount;
                    totalCount = totalItemCount;
                }
            });

            // 채팅 서버와 소켓 연결을 하는 부분입니다.
            socketChannel = SocketChannel.open();
            socketChannel.configureBlocking(true);
            socketChannel.connect(new InetSocketAddress(ip, port));

            // 소켓 연결된 서버에 내가 누구인지 알려줍니다.
            // 유저의 id와 서버에 접속한다는 의미인 type(join)을 채팅 서버로 전송합니다.
            jo.put("userId", id);
            jo.put("type", "join");
            socketChannel.socket().getOutputStream().write(jo.toString().getBytes("EUC-KR"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        LecChatReceiver lcr = new LecChatReceiver(lcContext);
        lcr.start();
    }

    public void joinRoom() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    jo.put("userId", id);
                    jo.put("type", "enter_room");
                    jo.put("roomId", rId);

                    socketChannel.socket().getOutputStream().write(jo.toString().getBytes("EUC-KR"));
                } catch (JSONException e) {
                    e.printStackTrace();
                    Log.i("LecChatThread:", "joinRoom:JSONException:error:" + e.getMessage());
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                    Log.i("LecChatThread:", "joinRoom:UnsupportedEncodingException:error:" + e.getMessage());
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.i("LecChatThread:", "joinRoom:IOException:error:" + e.getMessage());
                }
            }
        }).start();
    }

    public void sendLecChat(String lc) {
        final String msg = lc;
        try {
            jo.put("userId", id);
            jo.put("type", "send_room");
            jo.put("currentRoom", rId);
            jo.put("content", lc);

            socketChannel.socket().getOutputStream().write(jo.toString().getBytes("EUC-KR"));

            handler.post(new Runnable() {
                @Override
                public void run() {
                    LecChat lc = new LecChat(id, msg);
                    lcAdapter.addItem(lc);
                    lclv.setAdapter(lcAdapter);
                }
            });
        } catch (JSONException e) {
            e.printStackTrace();
            Log.i("LecChatThread:", "sendLecChat:JSONException:error:" + e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
            Log.i("LecChatThread:", "sendLecChat:IOException:error:" + e.getMessage());
        }
    }

    public void exitRoom() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    jo.put("userId", id);
                    jo.put("type", "exit_room");
                    jo.put("roomId", rId);

                    socketChannel.socket().getOutputStream().write(jo.toString().getBytes("EUC-KR"));

                    if (socketChannel.isConnected()) {
                        socketChannel.finishConnect();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    Log.i("LecChatThread:", "exitRoom:JSONException:error:" + e.getMessage());
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                    Log.i("LecChatThread:", "exitRoom:UnsupportedEncodingException:error:" + e.getMessage());
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.i("LecChatThread:", "exitRoom:IOException:error:" + e.getMessage());
                }
            }
        }).start();
    }

    public class LecChatReceiver extends Thread {

        Context lcrContext;

        String userid;
        String content;

        LecChatReceiver(Context c) {
            lcrContext = c;
        }

        public void run() {
            while (true) {
                ByteBuffer byteBuffer = ByteBuffer.allocate(256);
                try {
                    int readByteCount = socketChannel.read(byteBuffer);
                    Log.d("LecChatThread:", "readByteCount:" + readByteCount);

                    if (readByteCount == -1) {
                        throw new IOException();
                    }
                    byteBuffer.flip();
                    Charset charset = Charset.forName("EUC-KR");
                    JSONObject ob = new JSONObject(charset.decode(byteBuffer).toString());
                    userid = ob.getString("userId");
                    content = ob.getString("content");
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            LecChat lecChat = new LecChat(userid, content);
                            lcAdapter.addItem(lecChat);
                            lcAdapter.notifyDataSetChanged();
                            if(firVisibleNum+visibleCount==totalCount){
                                lclv.setSelection(totalCount);
                            } else {
                                lclv.setSelection(firVisibleNum+visibleCount);
                            }
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public class AlreadyReceivedChat extends Thread{

        AlreadyReceivedChat(){
            try {
                jo.put("id", id);
                jo.put("rId", rId);
                jo.put("paging", paging);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        public void run(){

            URL url = null;
            try {
                url = new URL("http://www.o-ddang.com/theteacher/removePic.php");
                HttpURLConnection httpURLConnection = (HttpURLConnection)url.openConnection();

                httpURLConnection.setDefaultUseCaches(false);
                httpURLConnection.setDoInput(true);
                httpURLConnection.setDoOutput(true);
                httpURLConnection.setRequestMethod("POST");

                httpURLConnection.setRequestProperty("Accept", "application/json");
                httpURLConnection.setRequestProperty("Content-type", "application/json");

                OutputStream os = httpURLConnection.getOutputStream();
                os.write(jo.toString().getBytes());
                os.flush();

                InputStreamReader tmp = new InputStreamReader(httpURLConnection.getInputStream(), "EUC-KR");
                BufferedReader reader = new BufferedReader(tmp);
                StringBuilder builder = new StringBuilder();
                String str;
                while ((str = reader.readLine()) != null) {
                    builder.append(str + "\n");
                }
                String result = builder.toString();
                Log.i("LecChatThread:", "AlreadyReceivedChat:"+result);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (ProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }
}