package com.example.theteacher;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.widget.AbsListView;
import android.widget.ListView;

import org.json.JSONArray;
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

    // Paging 기준(상위 2번쨰 메시지를 확인했을 때)에 도달했을 때 요청을 한 번만 할 수 있도록 구분하기 위한 변수입니다.
    // paging 요청을 보낸 후 기다리고 있는 상태일 경우 true,
    // 요청이 끝나서 모든 처리가 완료되었으면 false 값을 갖습니다.
    // 즉, true 일 때는 요청을 더 보내지 않고, false 일 때만 요청을 보냅니다.
    boolean isPaging = false;

    LecChatThread(Context c, ListView lv, LecChat_Adapter a, String r) {
        handler = new Handler();
        jo = new JSONObject();

        lcContext = c;
        lclv = lv;
        lcAdapter = a;
        rId = r;

        paging = 10;
        lclv.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {

            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                firVisibleNum = firstVisibleItem;
                visibleCount = visibleItemCount;
                totalCount = totalItemCount;

                // paging 구분 변수가 false이면서 리스트뷰 위에 남은 아이템 수가 1개일 때
                // 서버로 저장되어 있는 메시지를 더 받겠다는 요청을 보냅니다.
                if(firstVisibleItem<2 && isPaging==false){
                    RequestOldLecChat rolc = new RequestOldLecChat();
                    rolc.start();
                }
            }
        });

        SharedPreferences sp = lcContext.getSharedPreferences("profile", Context.MODE_PRIVATE);
        id = sp.getString("id", "");
    }

    @Override
    public void run() {
        try {
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

    // 채팅 서버에 누군지 알려준 후, 어떤 방에 입장했는지 알려주는 함수입니다.
    // rId가 채팅 방 이름을 나타내는 변수입니다.
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

    // 메시지를 전송하면 실행되는 함수입니다.
    // 어떤 유저(id)가 어떤 방(rId)에 어떤 내용(lc)을 보내는지(send_room) 전달하는 기능을 합니다.
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

    // 강의가 끝나고 방에서 나가게 되면 채팅방에서 나간다는 신호를 채팅 서버로 전송합니다.
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

    // 채팅 서버에 연결되면 다른 유저들이 보내는 메시지를 받을 수 있도록 기다리고 있는 '채팅 메시지 수신자' 역할을 하는 Thread입니다.
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

    // 기존에 저장되어 있는 채팅 메시지가 있는지 요청하는 부분입니다.
    // 처음 방에 접속했을 때, 리스트뷰에서 상위 안 본 메시지가 1개일 때 실행됩니다.
    public class RequestOldLecChat extends Thread{

        URL url = null;
        String result = null;

        RequestOldLecChat(){
            // 두 번 요청하지 않도록 isPaging 값을 true로 바꿉니다.
            // false일 때만 요청을 받기 때문입니다.
            isPaging = true;
            try {
                jo.put("id", id);
                jo.put("rId", rId);
                jo.put("paging", paging);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        public void run(){
            try {
                url = new URL("http://www.o-ddang.com/theteacher/getLecChat.php");
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
                result = builder.toString();
                Log.i("LecChatThread:", "AlreadyReceivedChat:"+result);

                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        try{
                            JSONObject jo1 = new JSONObject(result);
                            result = jo1.getString("process");
                            JSONArray array = jo1.getJSONArray("oldLecChat");

                            if(result.equals("ResponseSuccess")){
                                String userId = null;
                                String content = null;
                                for(int n=0; n<array.length(); n++){
                                    String oldChat = array.getString(n);
                                    JSONObject ocJo = new JSONObject(oldChat);

                                    if(!TextUtils.isEmpty(ocJo.getString("userId"))){
                                        userId = ocJo.getString("userId");
                                    }
                                    if(!TextUtils.isEmpty(ocJo.getString("content"))){
                                        content = ocJo.getString("content");
                                    }
                                    LecChat lecture = new LecChat(userId, content);
                                    lcAdapter.reverseAddItem(lecture);
                                }
                                lcAdapter.notifyDataSetChanged();
                                lclv.setSelection(firVisibleNum+10);

                                // 기존에 불러온 값에서 10개씩 추가하여 불러오도록 해주는 부분입니다.
                                // 초기 paging 값은 10이고, 이 부분에서 요청이 완료될 때 마다 10씩 증가시킵니다.
                                paging += 10;
                                // 메시지 불러오는 요청을 성공적으로 끝냈기 때문에 재요청 할 수 있도록 isPaging 값을 false로 바꿉니다.
                                isPaging = false;
                            } else if(result.equals("ResponseFail")){
                                // 서버에 더 이상 저장되어 있는 메시지가 없는 경우 fail을 전달받습니다.
                                // 더 받을 메시지가 없기 때문에 더 요청도 하지 않도록 isPaging값을 true로 완전히 바꿉니다.
                                isPaging = true;
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                            Log.i("LecChatThread:", "RequestOldLecChat:JSONException:"+e.getMessage());
                        }
                    }
                });

            } catch (MalformedURLException e) {
                e.printStackTrace();
                Log.i("LecChatThread:", "RequestOldLecChat:MalformedURLException:"+e.getMessage());
            } catch (ProtocolException e) {
                e.printStackTrace();
                Log.i("LecChatThread:", "RequestOldLecChat:ProtocolException:"+e.getMessage());
            } catch (IOException e) {
                e.printStackTrace();
                Log.i("LecChatThread:", "RequestOldLecChat:IOException:"+e.getMessage());
            }

        }
    }

}