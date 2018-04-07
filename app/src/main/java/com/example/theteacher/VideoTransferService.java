package com.example.theteacher;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * Created by kimhj on 2018-03-24.
 */

// Socket을 이용하여 서버로 비디오 파일을 전송하는 Service Class입니다.
// RecordedEnroll_CustomDialog 에서 사용됩니다.
// 업로드하여 저장할 파일이름과 파일의 path를 받습니다.
public class VideoTransferService extends Service {

    // 연결될 서버의 IP와 Port 번호입니다.
    String ip = "115.71.232.230";
    int port = 5555;

    // 서버에 저장될 때 파일의 제목을 미리 정해서 알려주기 위해 사용되는 변수입니다.
    String reTitle;
    // 서버에 보낼 파일의 경로를 담는 변수입니다.
    String rePath;
    // 서버에 저장될 파일의 파일의 크기를 담고있는 변수입니다.
    long size;

    // 현재 진행 상황을 파악하기 위해 사용되는 변수들입니다.
    // bytesRead는 실제 파일에서 읽어 들인 byte를 담기 때문에 bytesRead를 사용하여 파일을 모두 읽었는지 확인할 수 있습니다.
    int bytesRead;
    int count;

    // notification의 progressbar에 사용될 % 값을 담고 있는 변수입니다.
    // 현재 얼마나 업로드 되어있는지 %로 표현하는데 사용됩니다.
    int perc;

    // notification에 진행 상황을 %로 알려주기 위해 사용됩니다.
    NotificationManager mNotifyManager;
    NotificationCompat.Builder mBuilder;

    // 앱이 종료되었을 때 업로드중이면 이어서 전송할 수 있도록 START_CONTINUATION_MASK Flag를 담아서 onStartCommand의 return 값에 넣어줍니다.
    // 만약 전송이 완료된 상태라면 다른 파일을 또 업로드 할 수 있도록 이 service를 죽여야 합니다. 그래서 START_NOT_STICKY Flag를 담습니다.
    int returnValue;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        reTitle = intent.getStringExtra("videoTitle");
        rePath = intent.getStringExtra("videoPath");

        // 비디오 업로드를 마친 후 service가 종료되면 자동으로 재시작되지 않게 합니다.
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
        // 도중에 앱을 종료하더라도 지금 업로드 중인 파일은 마저 보낼 수 있도록 Flag 설정
        returnValue = START_CONTINUATION_MASK;

        bytesRead = 0;
        count = 0;

        mNotifyManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mBuilder = new NotificationCompat.Builder(getApplicationContext());
        mBuilder.setContentTitle("Video Upload").setSmallIcon(R.drawable.upload);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // 서버에 Socket 연결하는 부분입니다.
                    InetAddress server = InetAddress.getByName(ip);
                    Socket socket = new Socket(server, port);

                    // 서버에 저장될 파일의 파일의 크기를 담고있는 변수입니다.
                    size = new File(rePath).length();

                    // 파일이 클 경우 OOM이 발생할 수 있으므로 일정 크기로 나눠서 전송합니다.
                    // 여기서는 임시로 1024byte로 나눠서 전송했습니다.
                    byte[] buffer = new byte[1024];
                    // 주어진 경로의 파일을 담아두는 부분입니다.
                    FileInputStream fis = new FileInputStream(rePath);
                    // 소켓을 통해 서버로 비디오 데이터 조각을 전송하는 역할을 해주는 부분입니다.
                    DataOutputStream dos = new DataOutputStream(socket.getOutputStream());

                    // 먼저 저장될 파일의 제목을 전달합니다.
                    dos.writeUTF(reTitle);
                    // 두번째로 저장될 파일의 크기를 전달합니다.
                    String fileSize = String.valueOf(size);
                    dos.writeUTF(fileSize);
                    dos.flush();

                    // 실제로 비디오 데이터를 전송하는 부분입니다.
                    // fis에 담아둔 비디오 데이터를 1024byte의 크기 씩 읽어와서 전송합니다.
                    while((bytesRead = fis.read(buffer, 0, 1024)) != -1){
                        count += bytesRead;
                        dos.write(buffer);
                        dos.flush();

                        // notification에 progressbar를 띄워주는 부분입니다.
                        perc = (int)((float)count/(float)(Integer.parseInt(fileSize))*(float)100);
                        pgbar(perc);
                    }

                    // 열었던 연결들을 닫아주는 부분입니다.
                    fis.close();
                    dos.close();
                    socket.close();

                    // 전송이 완료되면 이 service가 재실행되지 않도록 flag 재설정 후 service 종료
                    returnValue = START_NOT_STICKY;
                    stopSelf();
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    // notification에 progressbar를 업데이트하는 함수입니다.
    // 입력받는 int p 값에 따라 %가 차오릅니다.
    public void pgbar(int p){
        if(p==100){
            // 100%에 도달하면 progresbar를 사라지게 합니다.
            mBuilder.setContentText("Upload Video complete").setProgress(0,0,false);
            mNotifyManager.notify(0, mBuilder.build());
            mNotifyManager.cancel(0);
        } else {
            // 프로그레스 바의 최고값(100)을 설정하고 perc 변수에 따라 바의 진행 상태를 변하게 합니다.
            mBuilder.setContentText(p+" %").setProgress(100, p, false);
            mNotifyManager.notify(0, mBuilder.build());
        }
    }

}
