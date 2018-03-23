package com.example.theteacher;

import android.app.ProgressDialog;
import android.util.Log;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * Created by kimhj on 2018-03-23.
 */

// Socket을 이용하여 서버로 비디오 파일을 전송하는 Thread Class입니다.
// RecordedEnroll_CustomDialog 에서 사용됩니다.
// 업로드하여 저장할 파일이름과 파일의 path를 받습니다.
public class VideoSocketTransfer extends Thread {

    // 연결될 서버의 IP와 Port 번호입니다.
    String ip = "115.71.232.230";
    int port = 5555;

    // 서버에 저장될 때 파일의 제목을 미리 정해서 알려주기 위해 사용되는 변수입니다.
    String reTitle;
    // 서버에 보낼 파일의 경로를 담는 변수입니다.
    String rePath;

    // 업로드 되는 동안 돌아가고 있을 프로그레스입니다.
    ProgressDialog progressDialog;

    public VideoSocketTransfer(ProgressDialog pd, String ti, String pa){
        reTitle = ti;
        rePath = pa;

        progressDialog = pd;
    }

    @Override
    public void run() {
        super.run();
        try {
            // 서버에 Socket 연결하는 부분입니다.
            InetAddress server = InetAddress.getByName(ip);
            Socket socket = new Socket(server, port);

            // 서버에 저장될 파일의 파일의 크기를 담고있는 변수입니다.
            long size = new File(rePath).length();

            // 파일이 클 경우 OOM이 발생할 수 있으므로 일정 크기로 나눠서 전송합니다.
            // 여기서는 임시로 1024byte로 나눠서 전송했습니다.
            byte[] buffer = new byte[1024];
            // 주어진 경로의 파일을 담아두는 부분입니다.
            FileInputStream fis = new FileInputStream(rePath);
            // 소켓을 통해 서버로 비디오 데이터 조각을 전송하는 역할을 해주는 부분입니다.
            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
            int bytesRead = 0;
            int count = 0;

            // 먼저 저장될 파일의 제목을 전달합니다.
            dos.writeUTF(reTitle);
            // 두번째로 저장될 파일의 크기를 전달합니다.
            dos.writeUTF(String.valueOf(size));
            dos.flush();

            // 실제로 비디오 데이터를 전송하는 부분입니다.
            // fis에 담아둔 비디오 데이터를 1024byte의 크기 씩 읽어와서 전송합니다.
            while((bytesRead = fis.read(buffer, 0, 1024)) != -1){
                count += bytesRead;
                Log.i("bytesRead : ", String.valueOf(count));
                dos.write(buffer);
                dos.flush();
            }

            // 열었던 연결들을 닫아주는 부분입니다.
            fis.close();
            dos.close();
            socket.close();
            progressDialog.dismiss();

        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }




}
